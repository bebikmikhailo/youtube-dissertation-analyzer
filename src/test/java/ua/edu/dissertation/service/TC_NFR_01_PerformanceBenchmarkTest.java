package ua.edu.dissertation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_NFR_01_PerformanceBenchmarkTest {

    private static final int GENERATED_VIDEOS_COUNT = 5_000;
    private static final long MAX_ALLOWED_MILLIS = 3_000;
    private static final LocalDate FIRST_PUBLICATION_DATE = LocalDate.of(2020, 1, 1);
    private static final int PUBLICATION_PERIOD_DAYS = 6 * 365;

    private static final List<String> SPECIALTIES = List.of(
            "121 Інженерія програмного забезпечення",
            "122 Комп'ютерні науки",
            "072 Фінанси, банківська справа та страхування",
            "051 Економіка",
            "081 Право"
    );

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

    private final List<GeneratedVideo> generatedVideos = new ArrayList<>();

    private record GeneratedVideo(String youtubeVideoId, String specialty, String university,
                                  LocalDate publishedAt, int viewCount) {
    }

    @BeforeEach
    void setUp() {
        clearDatabase();
        generatedVideos.clear();
        generateDataset();
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    private void clearDatabase() {
        statisticsSnapshotRepository.deleteAllInBatch();
        defenseVideoRepository.deleteAllInBatch();
        dissertationRepository.deleteAllInBatch();
        councilRepository.deleteAllInBatch();
        channelRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("NFR-01: Складна фільтрація та сортування 5 000 відео виконується менш ніж за 3 секунди")
    void shouldFilterAndSortFiveThousandVideosWithinThreeSeconds() {
        long storedVideosCount = defenseVideoRepository.count();
        assertThat(storedVideosCount).isEqualTo(GENERATED_VIDEOS_COUNT);

        LocalDate periodStart = LocalDate.of(2023, 1, 1);
        LocalDate periodEnd = LocalDate.of(2024, 12, 31);

        VideoFilterCriteria criteria = VideoFilterCriteria.builder()
                .specialty("121")
                .councilOrUniversity("КПІ")
                .publishedFrom(periodStart)
                .publishedTo(periodEnd)
                .minViews(100)
                .maxViews(4_000)
                .searchText("захист")
                .sortField(SortField.VIEWS)
                .sortDirection(Sort.Direction.DESC)
                .build();

        long startedAt = System.nanoTime();
        List<DefenseVideo> result = videoQueryService.findVideos(criteria);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertThat(elapsedMillis).isLessThan(MAX_ALLOWED_MILLIS);

        Set<String> expectedIds = generatedVideos.stream()
                .filter(video -> {
                    boolean isTargetSpecialty = video.specialty().startsWith("121");
                    boolean isKpiCouncil = video.university().contains("КПІ");
                    boolean isInsidePeriod = !video.publishedAt().isBefore(periodStart)
                            && !video.publishedAt().isAfter(periodEnd);
                    boolean isInsideViewsRange = video.viewCount() >= 100 && video.viewCount() <= 4_000;
                    boolean matchesAllConditions = isTargetSpecialty && isKpiCouncil
                            && isInsidePeriod && isInsideViewsRange;
                    return matchesAllConditions;
                })
                .map(GeneratedVideo::youtubeVideoId)
                .collect(Collectors.toSet());

        Set<String> actualIds = result.stream()
                .map(DefenseVideo::getYoutubeVideoId)
                .collect(Collectors.toSet());

        assertThat(result).isNotEmpty();
        assertThat(actualIds).isEqualTo(expectedIds);
        assertThat(result)
                .extracting(DefenseVideo::getViewCount)
                .isSortedAccordingTo((first, second) -> Integer.compare(second, first));
    }

    @Test
    @DisplayName("NFR-01: Сортування повної вибірки 5 000 відео за кожним полем укладається в 3 секунди")
    void shouldSortWholeDatasetByEveryFieldWithinThreeSeconds() {
        for (SortField sortField : SortField.values()) {
            for (Sort.Direction direction : Sort.Direction.values()) {
                VideoFilterCriteria criteria = VideoFilterCriteria.builder()
                        .sortField(sortField)
                        .sortDirection(direction)
                        .build();

                long startedAt = System.nanoTime();
                List<DefenseVideo> result = videoQueryService.findVideos(criteria);
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

                assertThat(result).hasSize(GENERATED_VIDEOS_COUNT);
                assertThat(elapsedMillis).isLessThan(MAX_ALLOWED_MILLIS);
            }
        }
    }

    private void generateDataset() {
        Random random = new Random(42);

        List<Channel> channels = channelRepository.saveAll(List.of(
                Channel.builder().id("UC_KPI_MEDIA").name("КПІмедіа").build(),
                Channel.builder().id("UC_NUBIP_OFFICIAL").name("НУБіП України").build(),
                Channel.builder().id("UC_KNU_OFFICIAL").name("КНУ ім. Тараса Шевченка").build(),
                Channel.builder().id("UC_LP_OFFICIAL").name("Львівська політехніка").build()
        ));

        List<Council> councils = councilRepository.saveAll(List.of(
                Council.builder().name("Д 26.002.01").university("КПІ ім. Ігоря Сікорського").build(),
                Council.builder().name("ДФ 26.004.001").university("НУБіП України").build(),
                Council.builder().name("Д 26.001.05").university("КНУ імені Тараса Шевченка").build(),
                Council.builder().name("Д 35.052.01").university("Національний університет «Львівська політехніка»").build()
        ));

        List<Dissertation> dissertations = new ArrayList<>(GENERATED_VIDEOS_COUNT);
        for (int index = 0; index < GENERATED_VIDEOS_COUNT; index++) {
            int institutionIndex = random.nextInt(councils.size());
            Council council = councils.get(institutionIndex);
            Channel channel = channels.get(institutionIndex);
            String specialty = SPECIALTIES.get(random.nextInt(SPECIALTIES.size()));
            LocalDate publishedAt = FIRST_PUBLICATION_DATE.plusDays(random.nextInt(PUBLICATION_PERIOD_DAYS));
            int viewCount = random.nextInt(5_000);
            String youtubeVideoId = "bench_" + index;

            Dissertation dissertation = Dissertation.builder()
                    .authorFullName("Здобувач Номер " + index)
                    .topic("Тема дослідження " + index)
                    .specialty(specialty)
                    .council(council)
                    .build();

            DefenseVideo video = DefenseVideo.builder()
                    .youtubeVideoId(youtubeVideoId)
                    .title("Захист дисертації здобувача " + index)
                    .description("Публічний захист у раді " + council.getName())
                    .publishedAt(publishedAt)
                    .durationSeconds(1_800 + random.nextInt(9_000))
                    .viewCount(viewCount)
                    .likeCount(random.nextInt(300))
                    .commentCount(random.nextInt(50))
                    .hasSubtitles(random.nextBoolean())
                    .channel(channel)
                    .build();

            dissertation.addVideo(video);
            dissertations.add(dissertation);
            generatedVideos.add(new GeneratedVideo(youtubeVideoId, specialty, council.getUniversity(),
                    publishedAt, viewCount));
        }

        dissertationRepository.saveAll(dissertations);
    }
}
