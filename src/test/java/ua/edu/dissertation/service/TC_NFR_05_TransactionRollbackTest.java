package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TC_NFR_05_TransactionRollbackTest {

    @Autowired
    private DataCollectorService dataCollectorService;

    @Autowired
    private DefenseVideoRepository defenseVideoRepository;

    @Autowired
    private StatisticsSnapshotRepository statisticsSnapshotRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @BeforeEach
    void setUp() {
        statisticsSnapshotRepository.deleteAll();
        defenseVideoRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    @DisplayName("NFR-05: Відкат транзакції пакетного збереження при виникненні помилки посеред обробки")
    void shouldRollbackEntireBatchWhenProcessingFailsMidway() {
        YouTubeVideoDto firstValidDto = YouTubeVideoDto.builder()
                .id("tx_video_valid_1")
                .title("Перший захист дисертації у пакеті")
                .viewCount(120)
                .likeCount(10)
                .commentCount(2)
                .build();

        YouTubeVideoDto secondValidDto = YouTubeVideoDto.builder()
                .id("tx_video_valid_2")
                .title("Другий захист дисертації у пакеті")
                .viewCount(240)
                .likeCount(20)
                .commentCount(4)
                .build();

        YouTubeVideoDto invalidDto = YouTubeVideoDto.builder()
                .id("")
                .title("Некоректне відео без ідентифікатора")
                .viewCount(50)
                .build();

        List<YouTubeVideoDto> batch = List.of(firstValidDto, secondValidDto, invalidDto);

        assertThatThrownBy(() -> dataCollectorService.saveMetadata(batch))
                .isInstanceOf(IllegalArgumentException.class);

        long persistedVideosCount = defenseVideoRepository.count();
        boolean isDatabaseEmpty = persistedVideosCount == 0;
        assertThat(isDatabaseEmpty).isTrue();

        Optional<DefenseVideo> firstVideoOptional = defenseVideoRepository.findByYoutubeVideoId("tx_video_valid_1");
        boolean isFirstVideoAbsent = firstVideoOptional.isEmpty();
        assertThat(isFirstVideoAbsent).isTrue();

        Optional<DefenseVideo> secondVideoOptional = defenseVideoRepository.findByYoutubeVideoId("tx_video_valid_2");
        boolean isSecondVideoAbsent = secondVideoOptional.isEmpty();
        assertThat(isSecondVideoAbsent).isTrue();

        long persistedSnapshotsCount = statisticsSnapshotRepository.count();
        boolean hasNoOrphanSnapshots = persistedSnapshotsCount == 0;
        assertThat(hasNoOrphanSnapshots).isTrue();
    }

    @Test
    @DisplayName("NFR-05: Збереження попереднього стану бази даних при невдалій пакетній модифікації")
    void shouldPreservePreExistingRecordsWhenSubsequentBatchFails() {
        YouTubeVideoDto baselineDto = YouTubeVideoDto.builder()
                .id("tx_baseline_video")
                .title("Базове відео захисту")
                .viewCount(300)
                .likeCount(30)
                .commentCount(5)
                .build();

        dataCollectorService.saveMetadata(baselineDto);

        long countBeforeFailedBatch = defenseVideoRepository.count();
        boolean hasSingleBaselineRecord = countBeforeFailedBatch == 1;
        assertThat(hasSingleBaselineRecord).isTrue();

        YouTubeVideoDto corruptedUpdateDto = YouTubeVideoDto.builder()
                .id("tx_baseline_video")
                .title("Спроба пошкодження назви")
                .viewCount(9999)
                .build();

        YouTubeVideoDto invalidItemDto = YouTubeVideoDto.builder()
                .id(null)
                .title("Помилковий елемент пакету")
                .build();

        List<YouTubeVideoDto> batchWithFailure = new ArrayList<>();
        batchWithFailure.add(corruptedUpdateDto);
        batchWithFailure.add(invalidItemDto);

        assertThatThrownBy(() -> dataCollectorService.saveMetadata(batchWithFailure))
                .isInstanceOf(IllegalArgumentException.class);

        long countAfterFailedBatch = defenseVideoRepository.count();
        boolean isCountUnchanged = countAfterFailedBatch == 1;
        assertThat(isCountUnchanged).isTrue();

        DefenseVideo baselineAfterRollback = defenseVideoRepository
                .findByYoutubeVideoId("tx_baseline_video")
                .orElseThrow();

        boolean isTitleIntact = "Базове відео захисту".equals(baselineAfterRollback.getTitle());
        boolean isViewsIntact = baselineAfterRollback.getViewCount() == 300;
        boolean isLikesIntact = baselineAfterRollback.getLikeCount() == 30;

        assertThat(isTitleIntact).isTrue();
        assertThat(isViewsIntact).isTrue();
        assertThat(isLikesIntact).isTrue();

        long snapshotsCount = statisticsSnapshotRepository.count();
        boolean isSnapshotsCountIntact = snapshotsCount == 1;
        assertThat(isSnapshotsCountIntact).isTrue();
    }
}

