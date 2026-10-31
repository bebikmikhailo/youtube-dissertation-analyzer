package ua.edu.dissertation.service;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.DescriptiveStatisticsDto;
import ua.edu.dissertation.dto.MetricType;
import ua.edu.dissertation.dto.VideoMetricStatisticsDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_NFR_04_MathPrecisionTest {

    private static final double ALLOWED_RELATIVE_ERROR = 0.0001;

    private static final List<Integer> REFERENCE_SAMPLE_ODD = List.of(
            120, 245, 310, 480, 520, 780, 950, 1200, 1450, 1890, 2300, 3100, 4500, 6200, 9800
    );
    private static final double EXPECTED_AVERAGE_ODD = 33845.0 / 15.0;
    private static final double EXPECTED_MEDIAN_ODD = 1200.0;

    private static final List<Integer> REFERENCE_SAMPLE_EVEN = List.of(
            4500, 120, 9800, 310, 1890, 520, 6200, 780, 245, 950, 2300, 480, 1450, 3100
    );
    private static final double EXPECTED_AVERAGE_EVEN = 32645.0 / 14.0;
    private static final double EXPECTED_MEDIAN_EVEN = 1200.0;

    private static final List<Double> REFERENCE_SAMPLE_DECIMAL = List.of(
            14.25, 88.5, 3.125, 99.875, 45.5, 72.0, 56.75
    );
    private static final double EXPECTED_AVERAGE_DECIMAL = 380.0 / 7.0;
    private static final double EXPECTED_MEDIAN_DECIMAL = 56.75;

    @Autowired
    private AnalyzerService analyzerService;

    @Test
    @DisplayName("NFR-04: Розрахунок середнього для непарної вибірки відповідає еталону з точністю вище 0.01%")
    void shouldCalculateAverageForOddSampleWithinPrecisionThreshold() {
        double actualAverage = analyzerService.обчислитиСереднє(REFERENCE_SAMPLE_ODD);

        double relativeError = Math.abs(actualAverage - EXPECTED_AVERAGE_ODD) / EXPECTED_AVERAGE_ODD;
        boolean isPrecisionAcceptable = relativeError < ALLOWED_RELATIVE_ERROR;

        assertThat(isPrecisionAcceptable).isTrue();
        assertThat(actualAverage).isCloseTo(EXPECTED_AVERAGE_ODD, Percentage.withPercentage(0.01));
    }

    @Test
    @DisplayName("NFR-04: Розрахунок медіани для непарної вибірки відповідає еталону з точністю вище 0.01%")
    void shouldCalculateMedianForOddSampleWithinPrecisionThreshold() {
        double actualMedian = analyzerService.обчислитиМедіану(REFERENCE_SAMPLE_ODD);

        double relativeError = Math.abs(actualMedian - EXPECTED_MEDIAN_ODD) / EXPECTED_MEDIAN_ODD;
        boolean isPrecisionAcceptable = relativeError < ALLOWED_RELATIVE_ERROR;

        assertThat(isPrecisionAcceptable).isTrue();
        assertThat(actualMedian).isCloseTo(EXPECTED_MEDIAN_ODD, Percentage.withPercentage(0.01));
    }

    @Test
    @DisplayName("NFR-04: Розрахунок середнього для парної невідсортованої вибірки відповідає еталону з точністю вище 0.01%")
    void shouldCalculateAverageForEvenSampleWithinPrecisionThreshold() {
        double actualAverage = analyzerService.обчислитиСереднє(REFERENCE_SAMPLE_EVEN);

        double relativeError = Math.abs(actualAverage - EXPECTED_AVERAGE_EVEN) / EXPECTED_AVERAGE_EVEN;
        boolean isPrecisionAcceptable = relativeError < ALLOWED_RELATIVE_ERROR;

        assertThat(isPrecisionAcceptable).isTrue();
        assertThat(actualAverage).isCloseTo(EXPECTED_AVERAGE_EVEN, Percentage.withPercentage(0.01));
    }

    @Test
    @DisplayName("NFR-04: Розрахунок медіани для парної невідсортованої вибірки відповідає еталону з точністю вище 0.01%")
    void shouldCalculateMedianForEvenSampleWithinPrecisionThreshold() {
        double actualMedian = analyzerService.обчислитиМедіану(REFERENCE_SAMPLE_EVEN);

        double relativeError = Math.abs(actualMedian - EXPECTED_MEDIAN_EVEN) / EXPECTED_MEDIAN_EVEN;
        boolean isPrecisionAcceptable = relativeError < ALLOWED_RELATIVE_ERROR;

        assertThat(isPrecisionAcceptable).isTrue();
        assertThat(actualMedian).isCloseTo(EXPECTED_MEDIAN_EVEN, Percentage.withPercentage(0.01));
    }

    @Test
    @DisplayName("NFR-04: Розрахунок описової статистики для дробових чисел відповідає еталону з точністю вище 0.01%")
    void shouldCalculateDescriptiveStatisticsForDecimalSampleWithinPrecisionThreshold() {
        DescriptiveStatisticsDto stats = analyzerService.calculateDescriptiveStatistics(REFERENCE_SAMPLE_DECIMAL);

        double averageRelativeError = Math.abs(stats.getAverage() - EXPECTED_AVERAGE_DECIMAL) / EXPECTED_AVERAGE_DECIMAL;
        double medianRelativeError = Math.abs(stats.getMedian() - EXPECTED_MEDIAN_DECIMAL) / EXPECTED_MEDIAN_DECIMAL;

        boolean isAveragePrecisionAcceptable = averageRelativeError < ALLOWED_RELATIVE_ERROR;
        boolean isMedianPrecisionAcceptable = medianRelativeError < ALLOWED_RELATIVE_ERROR;

        assertThat(isAveragePrecisionAcceptable).isTrue();
        assertThat(isMedianPrecisionAcceptable).isTrue();
        assertThat(stats.getAverage()).isCloseTo(EXPECTED_AVERAGE_DECIMAL, Percentage.withPercentage(0.01));
        assertThat(stats.getMedian()).isCloseTo(EXPECTED_MEDIAN_DECIMAL, Percentage.withPercentage(0.01));
        assertThat(stats.getCount()).isEqualTo(REFERENCE_SAMPLE_DECIMAL.size());
        assertThat(stats.getMin()).isEqualTo(3.125);
        assertThat(stats.getMax()).isEqualTo(99.875);
    }

    @Test
    @DisplayName("NFR-04: Розрахунок середнього та медіани через сутності DefenseVideo відповідає еталону з високою точністю")
    void shouldCalculateStatisticsForVideoEntitiesWithinPrecisionThreshold() {
        Channel channel = Channel.builder().id("ch_test").name("КПІмедіа").build();
        Dissertation dissertation = Dissertation.builder()
                .id(100L)
                .authorFullName("Іваненко Іван Іванович")
                .topic("Дослідження алгоритмів")
                .specialty("121 Інженерія програмного забезпечення")
                .build();

        List<DefenseVideo> videos = List.of(
                DefenseVideo.builder().youtubeVideoId("vid_1").channel(channel).dissertation(dissertation).viewCount(100).likeCount(10).commentCount(2).build(),
                DefenseVideo.builder().youtubeVideoId("vid_2").channel(channel).dissertation(dissertation).viewCount(200).likeCount(20).commentCount(4).build(),
                DefenseVideo.builder().youtubeVideoId("vid_3").channel(channel).dissertation(dissertation).viewCount(300).likeCount(30).commentCount(6).build()
        );

        VideoMetricStatisticsDto overall = analyzerService.обчислитиЗагальнуСтатистику(videos);

        assertThat(overall.getViews().getAverage()).isEqualTo(200.0);
        assertThat(overall.getViews().getMedian()).isEqualTo(200.0);
        assertThat(overall.getLikes().getAverage()).isEqualTo(20.0);
        assertThat(overall.getLikes().getMedian()).isEqualTo(20.0);
        assertThat(overall.getComments().getAverage()).isEqualTo(4.0);
        assertThat(overall.getComments().getMedian()).isEqualTo(4.0);

        double viewsAverage = analyzerService.обчислитиСереднє(MetricType.VIEWS, videos);
        double viewsMedian = analyzerService.обчислитиМедіану(MetricType.VIEWS, videos);
        assertThat(viewsAverage).isEqualTo(200.0);
        assertThat(viewsMedian).isEqualTo(200.0);
    }
}

