package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.Council;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.VideoFilterCriteria.SortField;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.CouncilRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.DissertationRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VideoQueryServiceTest {

    @Autowired
    private VideoQueryService videoQueryService;

    @Autowired
    private DefenseVideoRepository defenseVideoRepository;

    @Autowired
    private DissertationRepository dissertationRepository;

    @Autowired
    private CouncilRepository councilRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private StatisticsSnapshotRepository statisticsSnapshotRepository;

    @BeforeEach
    void setUp() {
        statisticsSnapshotRepository.deleteAllInBatch();
        defenseVideoRepository.deleteAllInBatch();
        dissertationRepository.deleteAllInBatch();
        councilRepository.deleteAllInBatch();
        channelRepository.deleteAllInBatch();

        Channel kpiChannel = channelRepository.save(Channel.builder().id("UC_KPI").name("КПІмедіа").build());
        Channel nubipChannel = channelRepository.save(Channel.builder().id("UC_NUBIP").name("НУБіП України").build());

        Council kpiCouncil = councilRepository.save(Council.builder()
                .name("Д 26.002.01").university("КПІ ім. Ігоря Сікорського").build());
        Council nubipCouncil = councilRepository.save(Council.builder()
                .name("ДФ 26.004.001").university("НУБіП України").build());

        saveDefense("kpi_shevchenko", "Захист дисертації Шевченка Д.В.", "Шевченко Дмитро Володимирович",
                "Методи машинного навчання для виявлення кібератак", "121 Інженерія програмного забезпечення",
                kpiCouncil, kpiChannel, LocalDate.of(2024, 1, 18), 4500, 410, 34, 8);

        saveDefense("kpi_koval", "Захист дисертації Коваля І.П.", "Коваль Ігор Петрович",
                "Розподілені системи обробки даних", "122 Комп'ютерні науки",
                kpiCouncil, kpiChannel, LocalDate.of(2022, 6, 10), 3000, 120, 9, 1);

        saveDefense("nubip_labenko_p1", "Захист дисертації Лабенка О.М. (частина 1)", "Лабенко Олександр Миколайович",
                "Фінансове забезпечення інноваційного розвитку аграрних підприємств",
                "072 Фінанси, банківська справа та страхування",
                nubipCouncil, nubipChannel, LocalDate.of(2023, 11, 20), 8860, 256, 18, 4);
    }

    @Test
    @DisplayName("FR-08: Фільтрація за назвою каналу без урахування регістру")
    void shouldFilterByChannelNameIgnoringCase() {
        VideoFilterCriteria criteria = VideoFilterCriteria.builder().channelName("кпімедіа").build();

        List<DefenseVideo> result = videoQueryService.findVideos(criteria);

        assertThat(result).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactlyInAnyOrder("kpi_shevchenko", "kpi_koval");
    }

    @Test
    @DisplayName("FR-08: Фільтрація за назвою ЗВО або шифром спеціалізованої ради")
    void shouldFilterByUniversityOrCouncilCipher() {
        VideoFilterCriteria byUniversity = VideoFilterCriteria.builder().councilOrUniversity("НУБіП").build();
        VideoFilterCriteria byCipher = VideoFilterCriteria.builder().councilOrUniversity("Д 26.002.01").build();

        assertThat(videoQueryService.findVideos(byUniversity)).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactly("nubip_labenko_p1");
        assertThat(videoQueryService.findVideos(byCipher)).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactlyInAnyOrder("kpi_shevchenko", "kpi_koval");
    }

    @Test
    @DisplayName("FR-08: Поєднання фільтрів спеціальності, інтервалу дат та діапазону переглядів")
    void shouldCombineSpecialtyDateAndViewsFilters() {
        VideoFilterCriteria criteria = VideoFilterCriteria.builder()
                .specialty("121")
                .publishedFrom(LocalDate.of(2023, 1, 1))
                .publishedTo(LocalDate.of(2024, 12, 31))
                .minViews(300)
                .maxViews(500)
                .build();

        List<DefenseVideo> result = videoQueryService.findVideos(criteria);

        assertThat(result).extracting(DefenseVideo::getYoutubeVideoId).containsExactly("kpi_shevchenko");
    }

    @Test
    @DisplayName("FR-08: Повнотекстовий пошук за ПІБ здобувача, темою та назвою відео")
    void shouldSearchTextAcrossAuthorTopicAndTitle() {
        VideoFilterCriteria byAuthor = VideoFilterCriteria.builder().searchText("лабенко").build();
        VideoFilterCriteria byTopic = VideoFilterCriteria.builder().searchText("кібератак").build();
        VideoFilterCriteria byTitle = VideoFilterCriteria.builder().searchText("частина 1").build();

        assertThat(videoQueryService.findVideos(byAuthor)).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactly("nubip_labenko_p1");
        assertThat(videoQueryService.findVideos(byTopic)).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactly("kpi_shevchenko");
        assertThat(videoQueryService.findVideos(byTitle)).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactly("nubip_labenko_p1");
    }

    @Test
    @DisplayName("FR-08: Символи шаблону LIKE у пошуковому запиті трактуються буквально")
    void shouldTreatLikeWildcardsInSearchTextLiterally() {
        VideoFilterCriteria criteria = VideoFilterCriteria.builder().searchText("%").build();

        assertThat(videoQueryService.findVideos(criteria)).isEmpty();
    }

    @Test
    @DisplayName("FR-09: Сортування за кожним полем у напрямках ASC та DESC")
    void shouldSortByEveryFieldInBothDirections() {
        assertSortedOrder(SortField.VIEWS, Sort.Direction.DESC, "kpi_shevchenko", "nubip_labenko_p1", "kpi_koval");
        assertSortedOrder(SortField.VIEWS, Sort.Direction.ASC, "kpi_koval", "nubip_labenko_p1", "kpi_shevchenko");
        assertSortedOrder(SortField.LIKES, Sort.Direction.DESC, "kpi_shevchenko", "nubip_labenko_p1", "kpi_koval");
        assertSortedOrder(SortField.COMMENTS, Sort.Direction.ASC, "kpi_koval", "nubip_labenko_p1", "kpi_shevchenko");
        assertSortedOrder(SortField.PUBLISHED_AT, Sort.Direction.DESC, "kpi_shevchenko", "nubip_labenko_p1", "kpi_koval");
        assertSortedOrder(SortField.DURATION, Sort.Direction.ASC, "kpi_koval", "kpi_shevchenko", "nubip_labenko_p1");
    }

    @Test
    @DisplayName("FR-09: Посторінкова видача зберігає порядок сортування")
    void shouldReturnSortedPage() {
        VideoFilterCriteria criteria = VideoFilterCriteria.builder()
                .sortField(SortField.VIEWS)
                .sortDirection(Sort.Direction.DESC)
                .build();

        Page<DefenseVideo> firstPage = videoQueryService.findVideos(criteria, 0, 2);

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactly("kpi_shevchenko", "nubip_labenko_p1");
    }

    private void assertSortedOrder(SortField field, Sort.Direction direction, String... expectedOrder) {
        VideoFilterCriteria criteria = VideoFilterCriteria.builder()
                .sortField(field)
                .sortDirection(direction)
                .build();

        assertThat(videoQueryService.findVideos(criteria))
                .extracting(DefenseVideo::getYoutubeVideoId)
                .containsExactly(expectedOrder);
    }

    private void saveDefense(String youtubeVideoId, String title, String author, String topic, String specialty,
                             Council council, Channel channel, LocalDate publishedAt, int durationSeconds,
                             int views, int likes, int comments) {
        Dissertation dissertation = Dissertation.builder()
                .authorFullName(author)
                .topic(topic)
                .specialty(specialty)
                .council(council)
                .build();

        DefenseVideo video = DefenseVideo.builder()
                .youtubeVideoId(youtubeVideoId)
                .title(title)
                .description("Трансляція засідання ради " + council.getName())
                .publishedAt(publishedAt)
                .durationSeconds(durationSeconds)
                .viewCount(views)
                .likeCount(likes)
                .commentCount(comments)
                .channel(channel)
                .build();

        dissertation.addVideo(video);
        dissertationRepository.save(dissertation);
    }
}

