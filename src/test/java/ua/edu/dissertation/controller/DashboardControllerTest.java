package ua.edu.dissertation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.dissertation.dto.ChannelRatingDto;
import ua.edu.dissertation.dto.CorrelationAnalysisDto;
import ua.edu.dissertation.dto.CorrelationResultDto;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.DynamicsInterval;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.SpecialtyRatingDto;
import ua.edu.dissertation.dto.UniversityPresentationProfile;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.VideoPartDetailDto;
import ua.edu.dissertation.dto.ViewDynamicsPointDto;
import ua.edu.dissertation.dto.ViewDynamicsReportDto;
import ua.edu.dissertation.service.AnalyzerService;
import ua.edu.dissertation.service.DefenseCatalogService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}]");
    private static final String EM_DASH = "\u2014";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyzerService analyzerService;

    @MockitoBean
    private DefenseCatalogService defenseCatalogService;

    @Test
    @DisplayName("Відображення сторінки дашборду з коректними KPI-метриками")
    void dashboardPageRendersKpiMetrics() throws Exception {
        List<DefenseAggregationDto> sampleDefenses = createSampleDefenses();
        when(defenseCatalogService.findDefenses(any(VideoFilterCriteria.class))).thenReturn(sampleDefenses);
        when(analyzerService.calculateAverage(anyList())).thenReturn(1450.5);
        when(analyzerService.calculateMedian(anyList())).thenReturn(1400.0);
        stubDefaultAnalyzerResponses();

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attributeExists("summary", "topChannels", "topSpecialties", "viewDynamics", "correlations", "councilComparison", "chartsJson"))
                .andExpect(content().string(containsString("id=\"kpi-total-defenses\"")))
                .andExpect(content().string(containsString("id=\"kpi-total-views\"")))
                .andExpect(content().string(containsString("id=\"kpi-average-views\"")))
                .andExpect(content().string(containsString("id=\"kpi-median-views\"")))
                .andExpect(content().string(containsString("id=\"kpi-multipart-count\"")))
                .andExpect(content().string(containsString("1 450.5")))
                .andExpect(content().string(containsString("1 400.0")));
    }

    @Test
    @DisplayName("Інтеграція Chart.js: наявність контейнерів та скриптів графіків")
    void dashboardPageRendersChartContainers() throws Exception {
        when(defenseCatalogService.findDefenses(any(VideoFilterCriteria.class))).thenReturn(createSampleDefenses());
        stubDefaultAnalyzerResponses();

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"channelsChart\"")))
                .andExpect(content().string(containsString("id=\"specialtiesChart\"")))
                .andExpect(content().string(containsString("id=\"dynamicsChart\"")))
                .andExpect(content().string(containsString("id=\"correlationsChart\"")))
                .andExpect(content().string(containsString("id=\"dashboard-data\"")))
                .andExpect(content().string(containsString("chart.umd.min.js")));
    }

    @Test
    @DisplayName("FR-14: Відображення порівняння подання інформації радами топ-ЗВО")
    void dashboardPageRendersCouncilComparisonReport() throws Exception {
        when(defenseCatalogService.findDefenses(any(VideoFilterCriteria.class))).thenReturn(createSampleDefenses());
        stubDefaultAnalyzerResponses();

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Порівняння подання інформації радами топ-ЗВО")))
                .andExpect(content().string(containsString("КПІ ім. Ігоря Сікорського")))
                .andExpect(content().string(containsString("НУБіП України")))
                .andExpect(content().string(containsString("КНУ імені Тараса Шевченка")))
                .andExpect(content().string(containsString("Львівська політехніка")))
                .andExpect(content().string(containsString("Спільні риси подання інформації")))
                .andExpect(content().string(containsString("Специфічні відмінності між радами")))
                .andExpect(content().string(containsString("Підсумковий аналітичний висновок")))
                .andExpect(content().string(containsString("id=\"council-comparison-table\"")));
    }

    @Test
    @DisplayName("Відповідність вимогам локалізації та оформлення інтерфейсу (без емодзі та довгих тире)")
    void dashboardPageConformsToStylingRules() throws Exception {
        when(defenseCatalogService.findDefenses(any(VideoFilterCriteria.class))).thenReturn(createSampleDefenses());
        stubDefaultAnalyzerResponses();

        String html = mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        boolean hasUkrainianLang = html.contains("<html lang=\"uk\"");
        boolean hasViewport = html.contains("name=\"viewport\" content=\"width=device-width, initial-scale=1\"");
        boolean hasBootstrap = html.contains("bootstrap@5.3.3/dist/css/bootstrap.min.css");
        boolean hasBootstrapIcons = html.contains("bootstrap-icons");
        boolean hasNoEmDash = !html.contains(EM_DASH);
        boolean hasNoEmoji = !EMOJI_PATTERN.matcher(html).find();

        assertThat(hasUkrainianLang).isTrue();
        assertThat(hasViewport).isTrue();
        assertThat(hasBootstrap).isTrue();
        assertThat(hasBootstrapIcons).isTrue();
        assertThat(hasNoEmDash).isTrue();
        assertThat(hasNoEmoji).isTrue();
    }

    @Test
    @DisplayName("Коректна робота за відсутності даних у БД (порожній вибіг)")
    void dashboardHandlesEmptyCatalogGracefully() throws Exception {
        when(defenseCatalogService.findDefenses(any(VideoFilterCriteria.class))).thenReturn(Collections.emptyList());
        when(analyzerService.getTopChannelsByViews(anyInt())).thenReturn(Collections.emptyList());
        when(analyzerService.getTopSpecialtiesByPopularity(anyInt())).thenReturn(Collections.emptyList());
        when(analyzerService.buildViewDynamics(any(DynamicsInterval.class))).thenReturn(
                ViewDynamicsReportDto.builder()
                        .interval(DynamicsInterval.DAYS)
                        .points(Collections.emptyList())
                        .build()
        );
        when(analyzerService.calculateCorrelations()).thenReturn(
                CorrelationAnalysisDto.builder()
                        .allCorrelations(Collections.emptyList())
                        .totalVideosAnalyzed(0)
                        .build()
        );
        when(analyzerService.compareCouncils()).thenReturn(
                PresentationComparisonReport.builder()
                        .profiles(Collections.emptyList())
                        .commonFeatures(Collections.emptyList())
                        .differences(Collections.emptyList())
                        .analyticalConclusion("Немає даних")
                        .build()
        );

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("summary", org.hamcrest.Matchers.hasProperty("totalDefenses", org.hamcrest.Matchers.is(0))))
                .andExpect(content().string(containsString("id=\"kpi-total-defenses\"")));
    }

    private void stubDefaultAnalyzerResponses() {
        when(analyzerService.getTopChannelsByViews(anyInt())).thenReturn(List.of(
                ChannelRatingDto.builder().rank(1).channelName("КПІ медіа").totalViews(5200L).videoCount(5).averageViews(1040.0).build(),
                ChannelRatingDto.builder().rank(2).channelName("НУБіП Live").totalViews(3100L).videoCount(3).averageViews(1033.3).build()
        ));

        when(analyzerService.getTopSpecialtiesByPopularity(anyInt())).thenReturn(List.of(
                SpecialtyRatingDto.builder().rank(1).specialty("121 Інженерія програмного забезпечення").totalViews(4500L).defenseCount(4).averageViews(1125.0).build(),
                SpecialtyRatingDto.builder().rank(2).specialty("122 Комп'ютерні науки").totalViews(3800L).defenseCount(3).averageViews(1266.6).build()
        ));

        when(analyzerService.buildViewDynamics(any(DynamicsInterval.class))).thenReturn(
                ViewDynamicsReportDto.builder()
                        .interval(DynamicsInterval.DAYS)
                        .points(List.of(
                                ViewDynamicsPointDto.builder().period("01.05").totalViews(1000L).viewsGrowth(0L).build(),
                                ViewDynamicsPointDto.builder().period("02.05").totalViews(1500L).viewsGrowth(500L).build()
                        ))
                        .initialViews(1000L)
                        .latestViews(1500L)
                        .totalGrowth(500L)
                        .totalSnapshots(2)
                        .build()
        );

        when(analyzerService.calculateCorrelations()).thenReturn(
                CorrelationAnalysisDto.builder()
                        .durationCorrelation(CorrelationResultDto.builder()
                                .factorName("Тривалість відео")
                                .coefficient(0.42)
                                .strength("Помірна")
                                .interpretation("Помірна лінійна залежність")
                                .isPositive(true)
                                .build())
                        .allCorrelations(List.of(
                                CorrelationResultDto.builder()
                                        .factorName("Тривалість відео")
                                        .coefficient(0.42)
                                        .strength("Помірна")
                                        .interpretation("Помірна лінійна залежність")
                                        .isPositive(true)
                                        .build(),
                                CorrelationResultDto.builder()
                                        .factorName("Наявність субтитрів")
                                        .coefficient(0.25)
                                        .strength("Слабка")
                                        .interpretation("Слабкий позитивний вплив")
                                        .isPositive(true)
                                        .build()
                        ))
                        .totalVideosAnalyzed(7)
                        .build()
        );

        when(analyzerService.compareCouncils()).thenReturn(
                PresentationComparisonReport.builder()
                        .profiles(List.of(
                                UniversityPresentationProfile.builder()
                                        .universityName("КПІ ім. Ігоря Сікорського")
                                        .totalVideosAnalyzed(10)
                                        .titleHasAuthorPercentage(100.0)
                                        .titleHasTopicPercentage(20.0)
                                        .titleHasDegreePercentage(100.0)
                                        .descriptionHasStructuredLabelsPercentage(100.0)
                                        .descriptionHasCouncilCodePercentage(90.0)
                                        .descriptionHasSpecialtyCodePercentage(100.0)
                                        .multiPartPracticeDetected(false)
                                        .styleSummary("Лаконічний заголовок зі ступенем, структурований опис.")
                                        .build(),
                                UniversityPresentationProfile.builder()
                                        .universityName("НУБіП України")
                                        .totalVideosAnalyzed(8)
                                        .titleHasAuthorPercentage(100.0)
                                        .titleHasTopicPercentage(30.0)
                                        .titleHasDegreePercentage(50.0)
                                        .titleHasPartNumberPercentage(25.0)
                                        .descriptionHasStructuredLabelsPercentage(40.0)
                                        .descriptionHasCouncilCodePercentage(80.0)
                                        .descriptionHasSpecialtyCodePercentage(100.0)
                                        .multiPartPracticeDetected(true)
                                        .styleSummary("Практика поділу на частини, опис зв'язним текстом.")
                                        .build(),
                                UniversityPresentationProfile.builder()
                                        .universityName("КНУ імені Тараса Шевченка")
                                        .totalVideosAnalyzed(6)
                                        .titleHasAuthorPercentage(100.0)
                                        .titleHasTopicPercentage(100.0)
                                        .titleHasDegreePercentage(60.0)
                                        .descriptionHasStructuredLabelsPercentage(50.0)
                                        .descriptionHasCouncilCodePercentage(50.0)
                                        .descriptionHasSpecialtyCodePercentage(100.0)
                                        .multiPartPracticeDetected(false)
                                        .styleSummary("Повна назва теми безпосередньо в заголовку.")
                                        .build(),
                                UniversityPresentationProfile.builder()
                                        .universityName("Національний університет «Львівська політехніка»")
                                        .totalVideosAnalyzed(7)
                                        .titleHasAuthorPercentage(100.0)
                                        .titleHasTopicPercentage(100.0)
                                        .titleHasDegreePercentage(70.0)
                                        .descriptionHasStructuredLabelsPercentage(100.0)
                                        .descriptionHasCouncilCodePercentage(70.0)
                                        .descriptionHasSpecialtyCodePercentage(100.0)
                                        .multiPartPracticeDetected(false)
                                        .styleSummary("Комплексний стандарт подання: тема в назві, структурований опис.")
                                        .build()
                        ))
                        .commonFeatures(List.of("Усі досліджені ЗВО обов'язково публікують ПІБ здобувача."))
                        .differences(List.of("НУБіП активно практикує поділ захисту на окремі відеозаписи."))
                        .analyticalConclusion("Аналіз оформлення метаданих захистів дисертацій підтверджує варіативність підходів.")
                        .build()
        );
    }

    private List<DefenseAggregationDto> createSampleDefenses() {
        DefenseAggregationDto singlePart = DefenseAggregationDto.builder()
                .dissertationId(1L)
                .authorFullName("Іваненко Петро Сергійович")
                .topic("Методи комп'ютерного зору")
                .specialty("121 Інженерія програмного забезпечення")
                .councilName("ДФ 26.002.001")
                .universityName("КПІ ім. Ігоря Сікорського")
                .totalPartsCount(1)
                .totalViewCount(1200)
                .totalLikeCount(45)
                .totalCommentCount(12)
                .totalDurationSeconds(5400)
                .formattedTotalDuration("1 год 30 хв")
                .parts(List.of(
                        VideoPartDetailDto.builder()
                                .videoId(101L)
                                .youtubeVideoId("vidSingle1")
                                .partNumber(1)
                                .title("Захист PhD Іваненко П. С.")
                                .publishedAt(LocalDate.of(2025, 4, 10))
                                .durationSeconds(5400)
                                .viewCount(1200)
                                .likeCount(45)
                                .commentCount(12)
                                .channelName("КПІ медіа")
                                .build()
                ))
                .build();

        DefenseAggregationDto multiPart = DefenseAggregationDto.builder()
                .dissertationId(2L)
                .authorFullName("Лабенко Олександр Миколайович")
                .topic("Управління проектами в агробізнесі")
                .specialty("072 Фінанси, банківська справа та страхування")
                .councilName("Д 26.004.01")
                .universityName("НУБіП України")
                .totalPartsCount(2)
                .totalViewCount(1701)
                .totalLikeCount(60)
                .totalCommentCount(18)
                .totalDurationSeconds(7200)
                .formattedTotalDuration("2 год 0 хв")
                .parts(List.of(
                        VideoPartDetailDto.builder()
                                .videoId(201L)
                                .youtubeVideoId("vidMulti1")
                                .partNumber(1)
                                .title("Захист Лабенка О. М. (частина 1)")
                                .publishedAt(LocalDate.of(2025, 4, 15))
                                .durationSeconds(3600)
                                .viewCount(901)
                                .likeCount(35)
                                .commentCount(10)
                                .channelName("НУБіП Live")
                                .build(),
                        VideoPartDetailDto.builder()
                                .videoId(202L)
                                .youtubeVideoId("vidMulti2")
                                .partNumber(2)
                                .title("Захист Лабенка О. М. (частина 2)")
                                .publishedAt(LocalDate.of(2025, 4, 15))
                                .durationSeconds(3600)
                                .viewCount(800)
                                .likeCount(25)
                                .commentCount(8)
                                .channelName("НУБіП Live")
                                .build()
                ))
                .build();

        return List.of(singlePart, multiPart);
    }
}
