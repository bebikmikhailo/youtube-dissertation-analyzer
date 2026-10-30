package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_FR_03_StatisticsSnapshotTest {

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
    @DisplayName("FR-03: Створення початкового знімка статистики при збереженні відео з поточним часом")
    void shouldCreateInitialStatisticsSnapshotWithCurrentTimestampWhenVideoSaved() {
        LocalDateTime timeBefore = LocalDateTime.now().minusSeconds(2);

        YouTubeVideoDto videoDto = YouTubeVideoDto.builder()
                .id("snapshot_test_initial")
                .title("Захист кандидатської дисертації")
                .durationSeconds(3600)
                .viewCount(220)
                .likeCount(16)
                .commentCount(5)
                .build();

        DefenseVideo savedVideo = dataCollectorService.saveMetadata(videoDto);

        LocalDateTime timeAfter = LocalDateTime.now().plusSeconds(2);

        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository
                .findByVideoYoutubeVideoIdOrderByCollectedAtAsc("snapshot_test_initial");

        boolean hasSingleSnapshot = snapshots.size() == 1;
        assertThat(hasSingleSnapshot).isTrue();

        StatisticsSnapshot snapshot = snapshots.get(0);

        boolean isViewsEqual = snapshot.getViews() == 220;
        boolean isLikesEqual = snapshot.getLikes() == 16;
        boolean isCommentsEqual = snapshot.getComments() == 5;

        LocalDateTime collectedAt = snapshot.getCollectedAt();
        boolean isTimestampAfterOrEqualBefore = !collectedAt.isBefore(timeBefore);
        boolean isTimestampBeforeOrEqualAfter = !collectedAt.isAfter(timeAfter);

        assertThat(isViewsEqual).isTrue();
        assertThat(isLikesEqual).isTrue();
        assertThat(isCommentsEqual).isTrue();
        assertThat(isTimestampAfterOrEqualBefore).isTrue();
        assertThat(isTimestampBeforeOrEqualAfter).isTrue();
    }

    @Test
    @DisplayName("FR-03: Створення нового знімка при кожному оновленні показників статистики")
    void shouldCreateNewSnapshotWhenStatisticsAreUpdated() {
        YouTubeVideoDto initialDto = YouTubeVideoDto.builder()
                .id("snapshot_test_updates")
                .title("Захист докторської дисертації")
                .durationSeconds(4500)
                .viewCount(100)
                .likeCount(8)
                .commentCount(2)
                .build();

        dataCollectorService.saveMetadata(initialDto);

        YouTubeVideoDto updateDto = YouTubeVideoDto.builder()
                .id("snapshot_test_updates")
                .title("Захист докторської дисертації")
                .durationSeconds(4500)
                .viewCount(350)
                .likeCount(28)
                .commentCount(7)
                .build();

        dataCollectorService.saveMetadata(updateDto);

        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository
                .findByVideoYoutubeVideoIdOrderByCollectedAtAsc("snapshot_test_updates");

        boolean hasTwoSnapshots = snapshots.size() == 2;
        assertThat(hasTwoSnapshots).isTrue();

        StatisticsSnapshot firstSnapshot = snapshots.get(0);
        StatisticsSnapshot secondSnapshot = snapshots.get(1);

        boolean isFirstViewsCorrect = firstSnapshot.getViews() == 100;
        boolean isSecondViewsCorrect = secondSnapshot.getViews() == 350;
        boolean isSecondLikesCorrect = secondSnapshot.getLikes() == 28;
        boolean isSecondCommentsCorrect = secondSnapshot.getComments() == 7;

        boolean isChronological = !firstSnapshot.getCollectedAt().isAfter(secondSnapshot.getCollectedAt());

        assertThat(isFirstViewsCorrect).isTrue();
        assertThat(isSecondViewsCorrect).isTrue();
        assertThat(isSecondLikesCorrect).isTrue();
        assertThat(isSecondCommentsCorrect).isTrue();
        assertThat(isChronological).isTrue();
    }

    @Test
    @DisplayName("FR-03: Створення знімка статистики з явно заданою часовою міткою")
    void shouldCreateSnapshotViaExplicitMethodCallWithCustomTimestamp() {
        YouTubeVideoDto videoDto = YouTubeVideoDto.builder()
                .id("snapshot_test_custom_time")
                .title("Відео захисту для перевірки дати")
                .durationSeconds(3200)
                .viewCount(500)
                .likeCount(40)
                .commentCount(10)
                .build();

        DefenseVideo video = dataCollectorService.saveMetadata(videoDto);

        LocalDateTime historicalTimestamp = LocalDateTime.of(2023, 12, 1, 12, 0, 0);
        StatisticsSnapshot snapshot = dataCollectorService.createStatisticsSnapshot(video, historicalTimestamp);

        boolean isSnapshotSaved = snapshot.getId() != null;
        boolean isTimestampExact = historicalTimestamp.equals(snapshot.getCollectedAt());
        boolean isViewsExact = snapshot.getViews() == 500;

        assertThat(isSnapshotSaved).isTrue();
        assertThat(isTimestampExact).isTrue();
        assertThat(isViewsExact).isTrue();
    }

    @Test
    @DisplayName("FR-03: Накопичення історії знімків для побудови динаміки переглядів")
    void shouldAccumulateSnapshotsHistoryOverTime() {
        String videoId = "snapshot_test_dynamics";

        YouTubeVideoDto firstDto = YouTubeVideoDto.builder()
                .id(videoId)
                .title("Аналіз динаміки переглядів")
                .viewCount(100)
                .likeCount(5)
                .commentCount(1)
                .build();
        dataCollectorService.saveMetadata(firstDto);

        YouTubeVideoDto secondDto = YouTubeVideoDto.builder()
                .id(videoId)
                .title("Аналіз динаміки переглядів")
                .viewCount(250)
                .likeCount(15)
                .commentCount(3)
                .build();
        dataCollectorService.saveMetadata(secondDto);

        YouTubeVideoDto thirdDto = YouTubeVideoDto.builder()
                .id(videoId)
                .title("Аналіз динаміки переглядів")
                .viewCount(600)
                .likeCount(45)
                .commentCount(12)
                .build();
        dataCollectorService.saveMetadata(thirdDto);

        List<StatisticsSnapshot> history = statisticsSnapshotRepository
                .findByVideoYoutubeVideoIdOrderByCollectedAtAsc(videoId);

        boolean hasThreeEntries = history.size() == 3;
        assertThat(hasThreeEntries).isTrue();

        boolean isGrowingViews = history.get(0).getViews() < history.get(1).getViews()
                && history.get(1).getViews() < history.get(2).getViews();
        assertThat(isGrowingViews).isTrue();

        boolean isFinalViewsExpected = history.get(2).getViews() == 600;
        assertThat(isFinalViewsExpected).isTrue();
    }
}

