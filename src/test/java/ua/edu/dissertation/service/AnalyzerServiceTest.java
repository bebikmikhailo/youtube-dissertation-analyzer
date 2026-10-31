package ua.edu.dissertation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.ChannelRatingDto;
import ua.edu.dissertation.dto.DefenseDiscussionRatingDto;
import ua.edu.dissertation.dto.MetricType;
import ua.edu.dissertation.dto.SpecialtyRatingDto;
import ua.edu.dissertation.dto.SystemRatingsDto;
import ua.edu.dissertation.dto.VideoMetricStatisticsDto;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnalyzerServiceTest {

    @Autowired
    private AnalyzerService analyzerService;

    @Test
    @DisplayName("FR-10: Статистика успішно розраховується з групуванням за каналами")
    void shouldGroupStatisticsByChannels() {
        Channel kpiChannel = Channel.builder().id("ch_kpi").name("КПІмедіа").build();
        Channel lpChannel = Channel.builder().id("ch_lp").name("Львівська політехніка").build();

        DefenseVideo video1 = DefenseVideo.builder()
                .youtubeVideoId("v1")
                .channel(kpiChannel)
                .viewCount(1000)
                .likeCount(50)
                .commentCount(10)
                .build();
        DefenseVideo video2 = DefenseVideo.builder()
                .youtubeVideoId("v2")
                .channel(kpiChannel)
                .viewCount(2000)
                .likeCount(70)
                .commentCount(20)
                .build();
        DefenseVideo video3 = DefenseVideo.builder()
                .youtubeVideoId("v3")
                .channel(lpChannel)
                .viewCount(500)
                .likeCount(15)
                .commentCount(5)
                .build();

        List<DefenseVideo> videos = List.of(video1, video2, video3);

        Map<String, VideoMetricStatisticsDto> statsByChannel = analyzerService.обчислитиСтатистикуЗаКаналами(videos);
        assertThat(statsByChannel).containsKeys("КПІмедіа", "Львівська політехніка");

        VideoMetricStatisticsDto kpiStats = statsByChannel.get("КПІмедіа");
        assertThat(kpiStats.getViews().getAverage()).isEqualTo(1500.0);
        assertThat(kpiStats.getViews().getMedian()).isEqualTo(1500.0);
        assertThat(kpiStats.getLikes().getAverage()).isEqualTo(60.0);
        assertThat(kpiStats.getComments().getAverage()).isEqualTo(15.0);

        Map<String, Double> viewsByChannel = analyzerService.обчислитиСереднєЗаКаналами(MetricType.VIEWS, videos);
        assertThat(viewsByChannel.get("КПІмедіа")).isEqualTo(1500.0);
        assertThat(viewsByChannel.get("Львівська політехніка")).isEqualTo(500.0);

        Map<String, Double> commentsMedianByChannel = analyzerService.обчислитиМедіануЗаКаналами(MetricType.COMMENTS, videos);
        assertThat(commentsMedianByChannel.get("КПІмедіа")).isEqualTo(15.0);
        assertThat(commentsMedianByChannel.get("Львівська політехніка")).isEqualTo(5.0);
    }

    @Test
    @DisplayName("FR-10: Статистика успішно розраховується з групуванням за спеціальностями")
    void shouldGroupStatisticsBySpecialty() {
        Dissertation diss121 = Dissertation.builder()
                .id(1L)
                .specialty("121 Інженерія програмного забезпечення")
                .build();
        Dissertation diss122 = Dissertation.builder()
                .id(2L)
                .specialty("122 Комп'ютерні науки")
                .build();

        DefenseVideo video1 = DefenseVideo.builder()
                .youtubeVideoId("v1")
                .dissertation(diss121)
                .viewCount(1000)
                .likeCount(40)
                .commentCount(8)
                .build();
        DefenseVideo video2 = DefenseVideo.builder()
                .youtubeVideoId("v2")
                .dissertation(diss121)
                .viewCount(600)
                .likeCount(20)
                .commentCount(4)
                .build();
        DefenseVideo video3 = DefenseVideo.builder()
                .youtubeVideoId("v3")
                .dissertation(diss122)
                .viewCount(2500)
                .likeCount(90)
                .commentCount(30)
                .build();

        List<DefenseVideo> videos = List.of(video1, video2, video3);

        Map<String, VideoMetricStatisticsDto> statsBySpecialty = analyzerService.обчислитиСтатистикуЗаСпеціальностями(videos);
        assertThat(statsBySpecialty).containsKeys("121 Інженерія програмного забезпечення", "122 Комп'ютерні науки");

        VideoMetricStatisticsDto specialty121Stats = statsBySpecialty.get("121 Інженерія програмного забезпечення");
        assertThat(specialty121Stats.getViews().getAverage()).isEqualTo(800.0);
        assertThat(specialty121Stats.getViews().getMedian()).isEqualTo(800.0);

        Map<String, Double> viewsBySpecialty = analyzerService.обчислитиСереднєЗаСпеціальностями(MetricType.VIEWS, videos);
        assertThat(viewsBySpecialty.get("122 Комп'ютерні науки")).isEqualTo(2500.0);
    }

    @Test
    @DisplayName("FR-13: Формування рейтингів каналів, спеціальностей та найбільш обговорюваних захистів")
    void shouldGenerateRatingsCorrectly() {
        Channel kpi = Channel.builder().id("ch_kpi").name("КПІмедіа").build();
        Channel lp = Channel.builder().id("ch_lp").name("Львівська політехніка").build();

        Dissertation dissA = Dissertation.builder()
                .id(10L)
                .authorFullName("Петренко Петро Петрович")
                .topic("Кібербезпека розподілених мереж")
                .specialty("125 Кібербезпека")
                .build();

        Dissertation dissB = Dissertation.builder()
                .id(20L)
                .authorFullName("Сидоренко Сидір Сидорович")
                .topic("Аналіз великих мовних моделей")
                .specialty("122 Комп'ютерні науки")
                .build();

        DefenseVideo v1 = DefenseVideo.builder()
                .youtubeVideoId("v1")
                .channel(kpi)
                .dissertation(dissA)
                .viewCount(1000)
                .likeCount(50)
                .commentCount(35)
                .build();
        DefenseVideo v2 = DefenseVideo.builder()
                .youtubeVideoId("v2")
                .channel(kpi)
                .dissertation(dissA)
                .viewCount(500)
                .likeCount(20)
                .commentCount(15)
                .build();
        DefenseVideo v3 = DefenseVideo.builder()
                .youtubeVideoId("v3")
                .channel(lp)
                .dissertation(dissB)
                .viewCount(3000)
                .likeCount(100)
                .commentCount(20)
                .build();

        List<DefenseVideo> videos = List.of(v1, v2, v3);

        SystemRatingsDto ratings = analyzerService.сформуватиРейтинг(5, videos);

        List<ChannelRatingDto> topChannels = ratings.getTopChannels();
        assertThat(topChannels).hasSize(2);
        assertThat(topChannels.get(0).getRank()).isEqualTo(1);
        assertThat(topChannels.get(0).getChannelName()).isEqualTo("Львівська політехніка");
        assertThat(topChannels.get(0).getTotalViews()).isEqualTo(3000L);
        assertThat(topChannels.get(1).getRank()).isEqualTo(2);
        assertThat(topChannels.get(1).getChannelName()).isEqualTo("КПІмедіа");
        assertThat(topChannels.get(1).getTotalViews()).isEqualTo(1500L);

        List<SpecialtyRatingDto> topSpecialties = ratings.getTopSpecialties();
        assertThat(topSpecialties).hasSize(2);
        assertThat(topSpecialties.get(0).getRank()).isEqualTo(1);
        assertThat(topSpecialties.get(0).getSpecialty()).isEqualTo("122 Комп'ютерні науки");
        assertThat(topSpecialties.get(0).getTotalViews()).isEqualTo(3000L);
        assertThat(topSpecialties.get(1).getRank()).isEqualTo(2);
        assertThat(topSpecialties.get(1).getSpecialty()).isEqualTo("125 Кібербезпека");
        assertThat(topSpecialties.get(1).getTotalViews()).isEqualTo(1500L);

        List<DefenseDiscussionRatingDto> topDiscussed = ratings.getTopDiscussedDefenses();
        assertThat(topDiscussed).hasSize(2);
        assertThat(topDiscussed.get(0).getRank()).isEqualTo(1);
        assertThat(topDiscussed.get(0).getDissertationId()).isEqualTo(10L);
        assertThat(topDiscussed.get(0).getTotalComments()).isEqualTo(50);
        assertThat(topDiscussed.get(0).getPartCount()).isEqualTo(2);

        assertThat(topDiscussed.get(1).getRank()).isEqualTo(2);
        assertThat(topDiscussed.get(1).getDissertationId()).isEqualTo(20L);
        assertThat(topDiscussed.get(1).getTotalComments()).isEqualTo(20);
        assertThat(topDiscussed.get(1).getPartCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Обробка порожніх або крайових значень не спричиняє винятків")
    void shouldHandleEmptyOrNullInputGracefully() {
        assertThat(analyzerService.обчислитиСереднє(List.of())).isEqualTo(0.0);
        assertThat(analyzerService.обчислитиМедіану(List.of())).isEqualTo(0.0);
        assertThat(analyzerService.обчислитиСереднє((List<Integer>) null)).isEqualTo(0.0);
        assertThat(analyzerService.обчислитиМедіану((List<Integer>) null)).isEqualTo(0.0);

        VideoMetricStatisticsDto emptyStats = analyzerService.calculateOverallStatistics(List.of());
        assertThat(emptyStats.getSampleSize()).isEqualTo(0);
        assertThat(emptyStats.getViews().getAverage()).isEqualTo(0.0);

        SystemRatingsDto emptyRatings = analyzerService.сформуватиРейтинг(5, List.of());
        assertThat(emptyRatings.getTopChannels()).isEmpty();
        assertThat(emptyRatings.getTopSpecialties()).isEmpty();
        assertThat(emptyRatings.getTopDiscussedDefenses()).isEmpty();
    }
}

