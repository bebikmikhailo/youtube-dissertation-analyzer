package ua.edu.dissertation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;
import ua.edu.dissertation.dto.DynamicsInterval;
import ua.edu.dissertation.dto.ViewDynamicsPointDto;
import ua.edu.dissertation.dto.ViewDynamicsReportDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_FR_12_DynamicsTest {

    @Autowired
    private AnalyzerService analyzerService;

    @Test
    @DisplayName("FR-12: Агрегація динаміки зростання переглядів за днями (DAYS)")
    void shouldAggregateViewDynamicsByDays() {
        DefenseVideo video = DefenseVideo.builder()
                .id(101L)
                .youtubeVideoId("dyn_v1")
                .title("Захист з динамікою за днями")
                .build();

        StatisticsSnapshot snapshotDay1Morning = StatisticsSnapshot.builder()
                .id(1L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .views(100)
                .likes(5)
                .comments(1)
                .build();

        StatisticsSnapshot snapshotDay1Evening = StatisticsSnapshot.builder()
                .id(2L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 1, 18, 0))
                .views(120)
                .likes(6)
                .comments(2)
                .build();

        StatisticsSnapshot snapshotDay2 = StatisticsSnapshot.builder()
                .id(3L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 2, 12, 0))
                .views(250)
                .likes(14)
                .comments(5)
                .build();

        StatisticsSnapshot snapshotDay3 = StatisticsSnapshot.builder()
                .id(4L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 3, 15, 0))
                .views(450)
                .likes(30)
                .comments(10)
                .build();

        List<StatisticsSnapshot> snapshots = List.of(
                snapshotDay1Morning,
                snapshotDay1Evening,
                snapshotDay2,
                snapshotDay3
        );

        ViewDynamicsReportDto report = analyzerService.побудуватиДинаміку(snapshots, DynamicsInterval.DAYS);

        assertThat(report).isNotNull();
        boolean hasThreeDays = report.getPoints().size() == 3;
        assertThat(hasThreeDays).isTrue();

        List<ViewDynamicsPointDto> points = report.getPoints();

        ViewDynamicsPointDto point1 = points.get(0);
        boolean isFirstPeriodCorrect = "2026-10-01".equals(point1.getPeriod());
        boolean isFirstViewsCorrect = point1.getTotalViews() == 120;
        boolean isFirstGrowthZero = point1.getViewsGrowth() == 0;
        boolean hasTwoSnapshotsInDay1 = point1.getSnapshotCount() == 2;
        assertThat(isFirstPeriodCorrect).isTrue();
        assertThat(isFirstViewsCorrect).isTrue();
        assertThat(isFirstGrowthZero).isTrue();
        assertThat(hasTwoSnapshotsInDay1).isTrue();

        ViewDynamicsPointDto point2 = points.get(1);
        boolean isSecondPeriodCorrect = "2026-10-02".equals(point2.getPeriod());
        boolean isSecondViewsCorrect = point2.getTotalViews() == 250;
        boolean isSecondGrowthCorrect = point2.getViewsGrowth() == 130;
        assertThat(isSecondPeriodCorrect).isTrue();
        assertThat(isSecondViewsCorrect).isTrue();
        assertThat(isSecondGrowthCorrect).isTrue();

        ViewDynamicsPointDto point3 = points.get(2);
        boolean isThirdPeriodCorrect = "2026-10-03".equals(point3.getPeriod());
        boolean isThirdViewsCorrect = point3.getTotalViews() == 450;
        boolean isThirdGrowthCorrect = point3.getViewsGrowth() == 200;
        assertThat(isThirdPeriodCorrect).isTrue();
        assertThat(isThirdViewsCorrect).isTrue();
        assertThat(isThirdGrowthCorrect).isTrue();

        boolean isTotalGrowthExact = report.getTotalGrowth() == 330;
        boolean isInitialViewsExact = report.getInitialViews() == 120;
        boolean isLatestViewsExact = report.getLatestViews() == 450;
        boolean isSnapshotCountExact = report.getTotalSnapshots() == 4;

        assertThat(isTotalGrowthExact).isTrue();
        assertThat(isInitialViewsExact).isTrue();
        assertThat(isLatestViewsExact).isTrue();
        assertThat(isSnapshotCountExact).isTrue();
    }

    @Test
    @DisplayName("FR-12: Агрегація динаміки зростання переглядів за тижнями (WEEKS)")
    void shouldAggregateViewDynamicsByWeeks() {
        DefenseVideo video = DefenseVideo.builder()
                .id(102L)
                .youtubeVideoId("dyn_v2")
                .title("Захист з динамікою за тижнями")
                .build();

        StatisticsSnapshot week1Snapshot = StatisticsSnapshot.builder()
                .id(10L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 1, 12, 0))
                .views(300)
                .build();

        StatisticsSnapshot week2Snapshot1 = StatisticsSnapshot.builder()
                .id(11L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 6, 10, 0))
                .views(500)
                .build();

        StatisticsSnapshot week2Snapshot2 = StatisticsSnapshot.builder()
                .id(12L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 9, 18, 0))
                .views(650)
                .build();

        StatisticsSnapshot week3Snapshot = StatisticsSnapshot.builder()
                .id(13L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 15, 12, 0))
                .views(1000)
                .build();

        List<StatisticsSnapshot> snapshots = List.of(
                week1Snapshot,
                week2Snapshot1,
                week2Snapshot2,
                week3Snapshot
        );

        ViewDynamicsReportDto report = analyzerService.побудуватиДинаміку(snapshots, DynamicsInterval.WEEKS);

        assertThat(report).isNotNull();
        boolean hasThreeWeeks = report.getPoints().size() == 3;
        assertThat(hasThreeWeeks).isTrue();

        ViewDynamicsPointDto weekPoint1 = report.getPoints().get(0);
        boolean isWeek1ViewsCorrect = weekPoint1.getTotalViews() == 300;
        boolean isWeek1GrowthZero = weekPoint1.getViewsGrowth() == 0;
        assertThat(isWeek1ViewsCorrect).isTrue();
        assertThat(isWeek1GrowthZero).isTrue();

        ViewDynamicsPointDto weekPoint2 = report.getPoints().get(1);
        boolean isWeek2ViewsCorrect = weekPoint2.getTotalViews() == 650;
        boolean isWeek2GrowthCorrect = weekPoint2.getViewsGrowth() == 350;
        assertThat(isWeek2ViewsCorrect).isTrue();
        assertThat(isWeek2GrowthCorrect).isTrue();

        ViewDynamicsPointDto weekPoint3 = report.getPoints().get(2);
        boolean isWeek3ViewsCorrect = weekPoint3.getTotalViews() == 1000;
        boolean isWeek3GrowthCorrect = weekPoint3.getViewsGrowth() == 350;
        assertThat(isWeek3ViewsCorrect).isTrue();
        assertThat(isWeek3GrowthCorrect).isTrue();

        boolean isTotalWeeklyGrowthCorrect = report.getTotalGrowth() == 700;
        assertThat(isTotalWeeklyGrowthCorrect).isTrue();
    }

    @Test
    @DisplayName("FR-12: Агрегація динаміки зростання переглядів за місяцями (MONTHS)")
    void shouldAggregateViewDynamicsByMonths() {
        DefenseVideo video = DefenseVideo.builder()
                .id(103L)
                .youtubeVideoId("dyn_v3")
                .title("Захист з динамікою за місяцями")
                .build();

        StatisticsSnapshot septSnapshot = StatisticsSnapshot.builder()
                .id(20L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 9, 20, 10, 0))
                .views(400)
                .build();

        StatisticsSnapshot octSnapshot = StatisticsSnapshot.builder()
                .id(21L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 10, 25, 14, 0))
                .views(900)
                .build();

        StatisticsSnapshot novSnapshot = StatisticsSnapshot.builder()
                .id(22L)
                .video(video)
                .collectedAt(LocalDateTime.of(2026, 11, 1, 9, 0))
                .views(1500)
                .build();

        List<StatisticsSnapshot> snapshots = List.of(septSnapshot, octSnapshot, novSnapshot);

        ViewDynamicsReportDto report = analyzerService.побудуватиДинаміку(snapshots, DynamicsInterval.MONTHS);

        assertThat(report).isNotNull();
        boolean hasThreeMonths = report.getPoints().size() == 3;
        assertThat(hasThreeMonths).isTrue();

        ViewDynamicsPointDto month1 = report.getPoints().get(0);
        boolean isMonth1Label = "2026-09".equals(month1.getPeriod());
        boolean isMonth1Views = month1.getTotalViews() == 400;
        assertThat(isMonth1Label).isTrue();
        assertThat(isMonth1Views).isTrue();

        ViewDynamicsPointDto month2 = report.getPoints().get(1);
        boolean isMonth2Label = "2026-10".equals(month2.getPeriod());
        boolean isMonth2Growth = month2.getViewsGrowth() == 500;
        assertThat(isMonth2Label).isTrue();
        assertThat(isMonth2Growth).isTrue();

        ViewDynamicsPointDto month3 = report.getPoints().get(2);
        boolean isMonth3Label = "2026-11".equals(month3.getPeriod());
        boolean isMonth3Growth = month3.getViewsGrowth() == 600;
        assertThat(isMonth3Label).isTrue();
        assertThat(isMonth3Growth).isTrue();

        boolean isTotalMonthlyGrowth = report.getTotalGrowth() == 1100;
        assertThat(isTotalMonthlyGrowth).isTrue();
    }

    @Test
    @DisplayName("FR-12: Коректна обробка порожнього списку знімків без виникнення винятків")
    void shouldHandleEmptySnapshotsGracefully() {
        ViewDynamicsReportDto report = analyzerService.побудуватиДинаміку(Collections.emptyList(), DynamicsInterval.DAYS);

        assertThat(report).isNotNull();
        boolean hasNoPoints = report.getPoints().isEmpty();
        boolean isInitialZero = report.getInitialViews() == 0;
        boolean isLatestZero = report.getLatestViews() == 0;
        boolean isGrowthZero = report.getTotalGrowth() == 0;
        boolean isSnapshotsZero = report.getTotalSnapshots() == 0;

        assertThat(hasNoPoints).isTrue();
        assertThat(isInitialZero).isTrue();
        assertThat(isLatestZero).isTrue();
        assertThat(isGrowthZero).isTrue();
        assertThat(isSnapshotsZero).isTrue();
    }
}

