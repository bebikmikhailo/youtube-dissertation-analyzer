package ua.edu.dissertation.service;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.dto.CorrelationAnalysisDto;
import ua.edu.dissertation.dto.CorrelationResultDto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_FR_11_CorrelationTest {

    private static final List<Integer> REFERENCE_DURATION_MINUTES = List.of(30, 60, 90, 120, 150);
    private static final List<Integer> REFERENCE_VIEWS = List.of(100, 200, 320, 390, 510);
    private static final double EXPECTED_REFERENCE_PEARSON_R = 0.9975095844;

    @Autowired
    private AnalyzerService analyzerService;

    @Test
    @DisplayName("TC-FR-11: Розрахунок коефіцієнта Пірсона на еталонній вибірці збігається з еталоном r ≈ 0.998")
    void shouldCalculatePearsonCorrelationForReferenceSample() {
        double actualR = analyzerService.обчислитиКоефіцієнтПірсона(REFERENCE_DURATION_MINUTES, REFERENCE_VIEWS);

        boolean isHighlyCorrelated = actualR > 0.99;
        boolean isCloseToReference = Math.abs(actualR - EXPECTED_REFERENCE_PEARSON_R) < 0.0001;

        assertThat(isHighlyCorrelated).isTrue();
        assertThat(isCloseToReference).isTrue();
        assertThat(actualR).isCloseTo(0.998, Offset.offset(0.001));

        String interpretation = analyzerService.інтерпретуватиКореляцію(actualR);
        boolean hasExpectedInterpretation = "Дуже сильний позитивний кореляційний зв'язок".equals(interpretation);
        assertThat(hasExpectedInterpretation).isTrue();

        String strength = analyzerService.отриматиСилуКореляції(actualR);
        boolean hasExpectedStrength = "Дуже сильна".equals(strength);
        assertThat(hasExpectedStrength).isTrue();
    }

    @Test
    @DisplayName("FR-11: Комплексний аналіз кореляцій для списку відео за трьома факторами")
    void shouldCalculateCorrelationsForVideoDataset() {
        LocalDate fixedToday = LocalDate.of(2026, 11, 1);

        DefenseVideo video1 = DefenseVideo.builder()
                .youtubeVideoId("vid_c1")
                .title("Захист 1")
                .viewCount(100)
                .durationSeconds(1800)
                .publishedAt(LocalDate.of(2026, 10, 22))
                .hasSubtitles(false)
                .build();

        DefenseVideo video2 = DefenseVideo.builder()
                .youtubeVideoId("vid_c2")
                .title("Захист 2")
                .viewCount(200)
                .durationSeconds(3600)
                .publishedAt(LocalDate.of(2026, 10, 12))
                .hasSubtitles(false)
                .build();

        DefenseVideo video3 = DefenseVideo.builder()
                .youtubeVideoId("vid_c3")
                .title("Захист 3")
                .viewCount(320)
                .durationSeconds(5400)
                .publishedAt(LocalDate.of(2026, 10, 2))
                .hasSubtitles(true)
                .build();

        DefenseVideo video4 = DefenseVideo.builder()
                .youtubeVideoId("vid_c4")
                .title("Захист 4")
                .viewCount(390)
                .durationSeconds(7200)
                .publishedAt(LocalDate.of(2026, 9, 22))
                .hasSubtitles(true)
                .build();

        DefenseVideo video5 = DefenseVideo.builder()
                .youtubeVideoId("vid_c5")
                .title("Захист 5")
                .viewCount(510)
                .durationSeconds(9000)
                .publishedAt(LocalDate.of(2026, 9, 12))
                .hasSubtitles(true)
                .build();

        List<DefenseVideo> videos = List.of(video1, video2, video3, video4, video5);

        CorrelationAnalysisDto report = analyzerService.обчислитиКореляції(videos, fixedToday);

        assertThat(report).isNotNull();
        boolean hasAnalyzedAllVideos = report.getTotalVideosAnalyzed() == 5;
        assertThat(hasAnalyzedAllVideos).isTrue();

        CorrelationResultDto durationResult = report.getDurationCorrelation();
        assertThat(durationResult).isNotNull();
        boolean hasDurationFactor = "Тривалість відео".equals(durationResult.getFactorName());
        boolean isDurationClose = Math.abs(durationResult.getCoefficient() - EXPECTED_REFERENCE_PEARSON_R) < 0.0001;
        assertThat(hasDurationFactor).isTrue();
        assertThat(isDurationClose).isTrue();
        assertThat(durationResult.getInterpretation()).isEqualTo("Дуже сильний позитивний кореляційний зв'язок");

        CorrelationResultDto ageResult = report.getAgeDaysCorrelation();
        assertThat(ageResult).isNotNull();
        boolean hasAgeFactor = "Дні з моменту публікації".equals(ageResult.getFactorName());
        boolean isAgePositive = ageResult.getCoefficient() > 0.99;
        assertThat(hasAgeFactor).isTrue();
        assertThat(isAgePositive).isTrue();

        CorrelationResultDto subtitlesResult = report.getSubtitlesCorrelation();
        assertThat(subtitlesResult).isNotNull();
        boolean hasSubtitlesFactor = "Наявність субтитрів".equals(subtitlesResult.getFactorName());
        boolean isSubtitlesPositive = subtitlesResult.getCoefficient() > 0.8;
        assertThat(hasSubtitlesFactor).isTrue();
        assertThat(isSubtitlesPositive).isTrue();

        List<CorrelationResultDto> allCorrelations = report.getAllCorrelations();
        boolean hasThreeFactors = allCorrelations.size() == 3;
        assertThat(hasThreeFactors).isTrue();
    }

    @Test
    @DisplayName("FR-11: Точне визначення строго негативної лінійної кореляції r = -1.0")
    void shouldCalculatePerfectNegativeCorrelation() {
        List<Integer> xValues = List.of(10, 20, 30, 40, 50);
        List<Integer> yValues = List.of(500, 400, 300, 200, 100);

        double actualR = analyzerService.обчислитиКоефіцієнтПірсона(xValues, yValues);

        boolean isExactNegativeOne = Math.abs(actualR - (-1.0)) < 0.0001;
        assertThat(isExactNegativeOne).isTrue();

        String interpretation = analyzerService.інтерпретуватиКореляцію(actualR);
        boolean hasNegativeInterpretation = "Дуже сильний негативний кореляційний зв'язок".equals(interpretation);
        assertThat(hasNegativeInterpretation).isTrue();
    }

    @Test
    @DisplayName("FR-11: Розрахунок кореляції для константних значень повертає 0.0 без ділення на нуль")
    void shouldReturnZeroWhenVarianceIsZero() {
        List<Integer> xValues = List.of(50, 50, 50, 50);
        List<Integer> yValues = List.of(100, 200, 300, 400);

        double actualR = analyzerService.обчислитиКоефіцієнтПірсона(xValues, yValues);

        boolean isZero = actualR == 0.0;
        assertThat(isZero).isTrue();

        String interpretation = analyzerService.інтерпретуватиКореляцію(actualR);
        boolean isNoCorrelation = "Кореляційний зв'язок відсутній".equals(interpretation);
        assertThat(isNoCorrelation).isTrue();
    }

    @Test
    @DisplayName("FR-11: Обробка порожніх даних та вибірок менше двох пар повертає 0.0")
    void shouldReturnZeroForEmptyOrInsufficientData() {
        double emptyR = analyzerService.обчислитиКоефіцієнтПірсона(Collections.emptyList(), Collections.emptyList());
        double singleItemR = analyzerService.обчислитиКоефіцієнтПірсона(List.of(10), List.of(20));

        boolean isEmptyZero = emptyR == 0.0;
        boolean isSingleZero = singleItemR == 0.0;

        assertThat(isEmptyZero).isTrue();
        assertThat(isSingleZero).isTrue();
    }

    @Test
    @DisplayName("FR-11: Градація вербальної інтерпретації за шкалою зв'язку")
    void shouldInterpretAllCorrelationStrengthCategories() {
        assertThat(analyzerService.інтерпретуватиКореляцію(0.95))
                .isEqualTo("Дуже сильний позитивний кореляційний зв'язок");
        assertThat(analyzerService.отриматиСилуКореляції(0.95))
                .isEqualTo("Дуже сильна");

        assertThat(analyzerService.інтерпретуватиКореляцію(0.78))
                .isEqualTo("Сильний позитивний кореляційний зв'язок");
        assertThat(analyzerService.отриматиСилуКореляції(0.78))
                .isEqualTo("Сильна");

        assertThat(analyzerService.інтерпретуватиКореляцію(0.55))
                .isEqualTo("Помірний позитивний кореляційний зв'язок");
        assertThat(analyzerService.отриматиСилуКореляції(0.55))
                .isEqualTo("Помірна");

        assertThat(analyzerService.інтерпретуватиКореляцію(0.35))
                .isEqualTo("Слабкий позитивний кореляційний зв'язок");
        assertThat(analyzerService.отриматиСилуКореляції(0.35))
                .isEqualTo("Слабка");

        assertThat(analyzerService.інтерпретуватиКореляцію(0.15))
                .isEqualTo("Дуже слабкий позитивний кореляційний зв'язок");
        assertThat(analyzerService.отриматиСилуКореляції(0.15))
                .isEqualTo("Дуже слабка");

        assertThat(analyzerService.інтерпретуватиКореляцію(-0.62))
                .isEqualTo("Помірний негативний кореляційний зв'язок");
    }
}

