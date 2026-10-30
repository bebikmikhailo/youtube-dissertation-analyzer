package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.Council;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.CouncilRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.DissertationRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_FR_02_MetadataUpsertTest {

    @Autowired
    private DataCollectorService dataCollectorService;

    @Autowired
    private DefenseVideoRepository defenseVideoRepository;

    @Autowired
    private StatisticsSnapshotRepository statisticsSnapshotRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private DissertationRepository dissertationRepository;

    @Autowired
    private CouncilRepository councilRepository;

    @BeforeEach
    void setUp() {
        statisticsSnapshotRepository.deleteAll();
        defenseVideoRepository.deleteAll();
        dissertationRepository.deleteAll();
        councilRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    @DisplayName("FR-02: Збереження метаданих нового відео у базу даних")
    void shouldPersistNewVideoMetadataWhenVideoDoesNotExistInDatabase() {
        YouTubeVideoDto newVideoDto = YouTubeVideoDto.builder()
                .id("video_fr02_new")
                .title("Захист дисертації Іванченка І.І.")
                .description("Трансляція засідання спеціалізованої вченої ради")
                .publishedAt(LocalDate.of(2024, 2, 10))
                .durationSeconds(4200)
                .viewCount(350)
                .likeCount(25)
                .commentCount(6)
                .hasSubtitles(true)
                .channelId("UC_CHANNEL_TEST")
                .channelTitle("Канал Університету")
                .build();

        DefenseVideo savedVideo = dataCollectorService.saveMetadata(newVideoDto);

        boolean isSavedNotNull = savedVideo != null;
        assertThat(isSavedNotNull).isTrue();

        Optional<DefenseVideo> foundOptional = defenseVideoRepository.findByYoutubeVideoId("video_fr02_new");
        boolean isVideoPresent = foundOptional.isPresent();
        assertThat(isVideoPresent).isTrue();

        DefenseVideo videoFromDb = foundOptional.get();
        boolean isTitleCorrect = "Захист дисертації Іванченка І.І.".equals(videoFromDb.getTitle());
        boolean isViewsCorrect = videoFromDb.getViewCount() == 350;
        boolean isLikesCorrect = videoFromDb.getLikeCount() == 25;
        boolean isCommentsCorrect = videoFromDb.getCommentCount() == 6;
        boolean hasSubtitles = videoFromDb.getHasSubtitles();
        boolean hasChannel = videoFromDb.getChannel() != null;

        assertThat(isTitleCorrect).isTrue();
        assertThat(isViewsCorrect).isTrue();
        assertThat(isLikesCorrect).isTrue();
        assertThat(isCommentsCorrect).isTrue();
        assertThat(hasSubtitles).isTrue();
        assertThat(hasChannel).isTrue();

        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository
                .findByVideoYoutubeVideoIdOrderByCollectedAtAsc("video_fr02_new");
        boolean hasInitialSnapshot = snapshots.size() == 1;
        assertThat(hasInitialSnapshot).isTrue();
    }

    @Test
    @DisplayName("FR-02: Оновлення існуючого відео без створення дубліката (upsert)")
    void shouldUpdateExistingVideoMetadataWithoutCreatingDuplicateRecord() {
        YouTubeVideoDto initialDto = YouTubeVideoDto.builder()
                .id("video_fr02_upsert")
                .title("Початкова назва захисту")
                .description("Початковий опис")
                .publishedAt(LocalDate.of(2024, 1, 15))
                .durationSeconds(3600)
                .viewCount(100)
                .likeCount(10)
                .commentCount(2)
                .hasSubtitles(false)
                .channelId("UC_TEST_UPSERT")
                .channelTitle("Канал Тестовий")
                .build();

        DefenseVideo initialSaved = dataCollectorService.saveMetadata(initialDto);
        Long initialDatabaseId = initialSaved.getId();

        long countAfterFirstSave = defenseVideoRepository.count();
        boolean isSingleVideoPresent = countAfterFirstSave == 1;
        assertThat(isSingleVideoPresent).isTrue();

        YouTubeVideoDto updatedDto = YouTubeVideoDto.builder()
                .id("video_fr02_upsert")
                .title("Оновлена офіційна назва захисту")
                .description("Оновлений розширений опис захисту")
                .publishedAt(LocalDate.of(2024, 1, 15))
                .durationSeconds(3600)
                .viewCount(480)
                .likeCount(42)
                .commentCount(14)
                .hasSubtitles(true)
                .channelId("UC_TEST_UPSERT")
                .channelTitle("Канал Тестовий")
                .build();

        DefenseVideo updatedResult = dataCollectorService.saveMetadata(updatedDto);

        long countAfterSecondSave = defenseVideoRepository.count();
        boolean isStillSingleVideo = countAfterSecondSave == 1;
        assertThat(isStillSingleVideo).isTrue();

        boolean isIdUnchanged = Objects.equals(updatedResult.getId(), initialDatabaseId);
        boolean isTitleUpdated = "Оновлена офіційна назва захисту".equals(updatedResult.getTitle());
        boolean isDescriptionUpdated = "Оновлений розширений опис захисту".equals(updatedResult.getDescription());
        boolean isViewCountUpdated = updatedResult.getViewCount() == 480;
        boolean isLikeCountUpdated = updatedResult.getLikeCount() == 42;
        boolean isCommentCountUpdated = updatedResult.getCommentCount() == 14;
        boolean isSubtitlesUpdated = updatedResult.getHasSubtitles();

        assertThat(isIdUnchanged).isTrue();
        assertThat(isTitleUpdated).isTrue();
        assertThat(isDescriptionUpdated).isTrue();
        assertThat(isViewCountUpdated).isTrue();
        assertThat(isLikeCountUpdated).isTrue();
        assertThat(isCommentCountUpdated).isTrue();
        assertThat(isSubtitlesUpdated).isTrue();

        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository
                .findByVideoYoutubeVideoIdOrderByCollectedAtAsc("video_fr02_upsert");
        boolean hasTwoSnapshots = snapshots.size() == 2;
        assertThat(hasTwoSnapshots).isTrue();

        boolean isFirstSnapshotViewsCorrect = snapshots.get(0).getViews() == 100;
        boolean isSecondSnapshotViewsCorrect = snapshots.get(1).getViews() == 480;
        assertThat(isFirstSnapshotViewsCorrect).isTrue();
        assertThat(isSecondSnapshotViewsCorrect).isTrue();
    }

    @Test
    @DisplayName("FR-02: Збереження зв'язку з дисертацією та номера частини при оновленні метаданих")
    void shouldPreserveDissertationAndPartNumberDuringUpsert() {
        Council council = Council.builder()
                .name("Спеціалізована вчена рада Д 26.002.02")
                .university("КПІ ім. Ігоря Сікорського")
                .build();
        council = councilRepository.save(council);

        Dissertation dissertation = Dissertation.builder()
                .authorFullName("Петренко Петро Петрович")
                .topic("Аналіз архітектур розподілених систем")
                .specialty("121 Інженерія програмного забезпечення")
                .council(council)
                .build();
        dissertation = dissertationRepository.save(dissertation);

        YouTubeVideoDto initialDto = YouTubeVideoDto.builder()
                .id("video_fr02_preserve")
                .title("Захист Петренка П.П. (частина 2)")
                .description("Опис")
                .durationSeconds(3000)
                .viewCount(150)
                .likeCount(12)
                .commentCount(3)
                .hasSubtitles(true)
                .build();

        DefenseVideo video = dataCollectorService.saveMetadata(initialDto);
        video.setPartNumber(2);
        dissertation.addVideo(video);
        defenseVideoRepository.save(video);

        YouTubeVideoDto refreshDto = YouTubeVideoDto.builder()
                .id("video_fr02_preserve")
                .title("Захист Петренка П.П. (частина 2) - Нове редагування")
                .description("Оновлений опис")
                .durationSeconds(3000)
                .viewCount(290)
                .likeCount(24)
                .commentCount(7)
                .hasSubtitles(true)
                .build();

        dataCollectorService.saveMetadata(refreshDto);

        DefenseVideo retrieved = defenseVideoRepository.findByYoutubeVideoId("video_fr02_preserve").orElseThrow();

        boolean isPartNumberPreserved = retrieved.getPartNumber() == 2;
        boolean isDissertationPreserved = retrieved.getDissertation() != null
                && Objects.equals(retrieved.getDissertation().getId(), dissertation.getId());
        boolean isViewsUpdated = retrieved.getViewCount() == 290;

        assertThat(isPartNumberPreserved).isTrue();
        assertThat(isDissertationPreserved).isTrue();
        assertThat(isViewsUpdated).isTrue();
    }

    @Test
    @DisplayName("FR-02: Оновлення назви каналу при отриманні свіжих даних")
    void shouldUpdateChannelTitleWhenMetadataChanges() {
        YouTubeVideoDto firstDto = YouTubeVideoDto.builder()
                .id("video_channel_test_1")
                .title("Захист 1")
                .channelId("UC_CH_COMMON")
                .channelTitle("Початкова назва каналу")
                .build();

        dataCollectorService.saveMetadata(firstDto);

        Channel channelFirst = channelRepository.findById("UC_CH_COMMON").orElseThrow();
        boolean isInitialTitleCorrect = "Початкова назва каналу".equals(channelFirst.getName());
        assertThat(isInitialTitleCorrect).isTrue();

        YouTubeVideoDto secondDto = YouTubeVideoDto.builder()
                .id("video_channel_test_2")
                .title("Захист 2")
                .channelId("UC_CH_COMMON")
                .channelTitle("Оновлена офіційна назва каналу")
                .build();

        dataCollectorService.saveMetadata(secondDto);

        long channelCount = channelRepository.count();
        boolean isSingleChannel = channelCount == 1;
        assertThat(isSingleChannel).isTrue();

        Channel channelSecond = channelRepository.findById("UC_CH_COMMON").orElseThrow();
        boolean isUpdatedTitleCorrect = "Оновлена офіційна назва каналу".equals(channelSecond.getName());
        assertThat(isUpdatedTitleCorrect).isTrue();
    }
}

