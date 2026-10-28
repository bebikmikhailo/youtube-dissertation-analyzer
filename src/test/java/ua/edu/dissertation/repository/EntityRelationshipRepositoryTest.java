package ua.edu.dissertation.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.Council;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EntityRelationshipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private CouncilRepository councilRepository;

    @Autowired
    private DissertationRepository dissertationRepository;

    @Autowired
    private DefenseVideoRepository defenseVideoRepository;

    @Autowired
    private StatisticsSnapshotRepository statisticsSnapshotRepository;

    @Test
    @DisplayName("Збереження дисертації з декількома частинами відео захисту")
    void shouldPersistDissertationWithMultipleVideoParts() {
        Council council = Council.builder()
                .name("Разова рада ДФ 26.004.001")
                .university("НУБіП України")
                .build();
        council = entityManager.persist(council);

        Channel channel = Channel.builder()
                .id("UC_TEST_NUBIP_CHANNEL")
                .name("НУБіП України Офіційний")
                .build();
        channel = entityManager.persist(channel);

        Dissertation dissertation = Dissertation.builder()
                .authorFullName("Лабенко Олександр Миколайович")
                .topic("Фінансове забезпечення інноваційного розвитку аграрних підприємств")
                .specialty("072 Фінанси, банківська справа та страхування")
                .council(council)
                .build();

        DefenseVideo partOne = DefenseVideo.builder()
                .youtubeVideoId("labenko_part_1")
                .title("Захист Лабенка О.М. (частина 1)")
                .description("Трансляція захисту дисертації в разовій раді ДФ 26.004.001")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(3600)
                .viewCount(210)
                .likeCount(15)
                .commentCount(4)
                .hasSubtitles(true)
                .partNumber(1)
                .channel(channel)
                .build();

        DefenseVideo partTwo = DefenseVideo.builder()
                .youtubeVideoId("labenko_part_2")
                .title("Захист Лабенка О.М. (частина 2)")
                .description("Продовження трансляції захисту дисертації")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(2700)
                .viewCount(140)
                .likeCount(9)
                .commentCount(2)
                .hasSubtitles(true)
                .partNumber(2)
                .channel(channel)
                .build();

        dissertation.addVideo(partOne);
        dissertation.addVideo(partTwo);

        Dissertation savedDissertation = dissertationRepository.saveAndFlush(dissertation);
        entityManager.clear();

        Optional<Dissertation> foundDissertationOptional = dissertationRepository.findById(savedDissertation.getId());
        boolean isDissertationFound = foundDissertationOptional.isPresent();
        assertThat(isDissertationFound).isTrue();

        Dissertation foundDissertation = foundDissertationOptional.get();
        List<DefenseVideo> attachedVideos = foundDissertation.getVideos();
        boolean hasExpectedVideosCount = attachedVideos.size() == 2;
        assertThat(hasExpectedVideosCount).isTrue();

        boolean containsPartOne = attachedVideos.stream()
                .anyMatch(v -> v.getPartNumber() == 1 && "labenko_part_1".equals(v.getYoutubeVideoId()));
        boolean containsPartTwo = attachedVideos.stream()
                .anyMatch(v -> v.getPartNumber() == 2 && "labenko_part_2".equals(v.getYoutubeVideoId()));

        assertThat(containsPartOne).isTrue();
        assertThat(containsPartTwo).isTrue();

        List<DefenseVideo> videosByDissertationId = defenseVideoRepository.findByDissertationId(savedDissertation.getId());
        boolean hasMatchingVideoListSize = videosByDissertationId.size() == 2;
        assertThat(hasMatchingVideoListSize).isTrue();
    }

    @Test
    @DisplayName("Збереження відео захисту з історією знімків статистики")
    void shouldPersistDefenseVideoWithStatisticsSnapshotsHistory() {
        Channel channel = Channel.builder()
                .id("UC_TEST_KPI_CHANNEL")
                .name("КПІмедіа")
                .build();
        entityManager.persist(channel);

        DefenseVideo video = DefenseVideo.builder()
                .youtubeVideoId("kpi_defense_101")
                .title("Захист дисертації зі спеціальності 121")
                .description("Запис публічного захисту")
                .publishedAt(LocalDate.of(2024, 1, 15))
                .durationSeconds(4200)
                .viewCount(320)
                .likeCount(25)
                .commentCount(6)
                .hasSubtitles(false)
                .partNumber(1)
                .channel(channel)
                .build();

        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 16, 10, 0);

        StatisticsSnapshot snapshotOne = StatisticsSnapshot.builder()
                .collectedAt(baseTime)
                .views(100)
                .likes(8)
                .comments(2)
                .build();

        StatisticsSnapshot snapshotTwo = StatisticsSnapshot.builder()
                .collectedAt(baseTime.plusDays(1))
                .views(220)
                .likes(18)
                .comments(5)
                .build();

        StatisticsSnapshot snapshotThree = StatisticsSnapshot.builder()
                .collectedAt(baseTime.plusDays(2))
                .views(320)
                .likes(25)
                .comments(6)
                .build();

        video.addSnapshot(snapshotOne);
        video.addSnapshot(snapshotTwo);
        video.addSnapshot(snapshotThree);

        DefenseVideo savedVideo = defenseVideoRepository.saveAndFlush(video);
        entityManager.clear();

        List<StatisticsSnapshot> savedSnapshots = statisticsSnapshotRepository
                .findByVideoYoutubeVideoIdOrderByCollectedAtAsc("kpi_defense_101");

        boolean hasThreeSnapshots = savedSnapshots.size() == 3;
        assertThat(hasThreeSnapshots).isTrue();

        StatisticsSnapshot firstSnapshot = savedSnapshots.get(0);
        StatisticsSnapshot lastSnapshot = savedSnapshots.get(2);

        boolean isInitialViewsCorrect = firstSnapshot.getViews() == 100;
        boolean isLatestViewsCorrect = lastSnapshot.getViews() == 320;
        boolean isTimelineChronological = firstSnapshot.getCollectedAt().isBefore(lastSnapshot.getCollectedAt());

        assertThat(isInitialViewsCorrect).isTrue();
        assertThat(isLatestViewsCorrect).isTrue();
        assertThat(isTimelineChronological).isTrue();
    }

    @Test
    @DisplayName("Збереження зв'язку спеціалізованої вченої ради та дисертацій")
    void shouldPersistCouncilWithDissertations() {
        Council council = Council.builder()
                .name("Д 26.002.01")
                .university("КПІ ім. Ігоря Сікорського")
                .build();
        council = councilRepository.saveAndFlush(council);

        Dissertation dissertationOne = Dissertation.builder()
                .authorFullName("Петренко Петро Петрович")
                .topic("Методи та засоби інтелектуального аналізу даних")
                .specialty("121 Інженерія програмного забезпечення")
                .council(council)
                .build();

        Dissertation dissertationTwo = Dissertation.builder()
                .authorFullName("Іваненко Іван Іванович")
                .topic("Моделі кібербезпеки розподілених систем")
                .specialty("122 Комп'ютерні науки")
                .council(council)
                .build();

        dissertationRepository.saveAndFlush(dissertationOne);
        dissertationRepository.saveAndFlush(dissertationTwo);
        entityManager.clear();

        List<Dissertation> dissertationsByCouncil = dissertationRepository.findByCouncilId(council.getId());
        boolean hasTwoDissertations = dissertationsByCouncil.size() == 2;
        assertThat(hasTwoDissertations).isTrue();
    }

    @Test
    @DisplayName("Каскадне видалення знімків статистики при видаленні відео")
    void shouldCascadeDeleteSnapshotsWhenVideoIsRemoved() {
        Channel channel = Channel.builder()
                .id("UC_TEST_CASCADE_CHANNEL")
                .name("Тестовий канал")
                .build();
        entityManager.persist(channel);

        DefenseVideo video = DefenseVideo.builder()
                .youtubeVideoId("cascade_test_video")
                .title("Відео для перевірки каскадного видалення")
                .durationSeconds(1800)
                .viewCount(50)
                .likeCount(3)
                .commentCount(0)
                .hasSubtitles(false)
                .partNumber(1)
                .channel(channel)
                .build();

        StatisticsSnapshot snapshot = StatisticsSnapshot.builder()
                .collectedAt(LocalDateTime.now())
                .views(50)
                .likes(3)
                .comments(0)
                .build();

        video.addSnapshot(snapshot);
        DefenseVideo savedVideo = defenseVideoRepository.saveAndFlush(video);
        Long videoId = savedVideo.getId();

        entityManager.clear();

        defenseVideoRepository.deleteById(videoId);
        defenseVideoRepository.flush();

        List<StatisticsSnapshot> remainingSnapshots = statisticsSnapshotRepository.findByVideoIdOrderByCollectedAtAsc(videoId);
        boolean areSnapshotsDeleted = remainingSnapshots.isEmpty();
        boolean isVideoDeleted = defenseVideoRepository.findById(videoId).isEmpty();

        assertThat(areSnapshotsDeleted).isTrue();
        assertThat(isVideoDeleted).isTrue();
    }
}
