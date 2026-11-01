package ua.edu.dissertation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.dissertation.client.youtube.exception.QuotaExceededException;
import ua.edu.dissertation.client.youtube.exception.YouTubeApiException;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.dto.CollectionResultDto;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.VideoFilterCriteria.SortField;
import ua.edu.dissertation.dto.VideoPartDetailDto;
import ua.edu.dissertation.service.DataCollectorService;
import ua.edu.dissertation.service.DefenseCatalogService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DefenseController.class)
class DefenseViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DefenseCatalogService defenseCatalogService;

    @MockitoBean
    private DataCollectorService dataCollectorService;

    @Test
    void rootPathRedirectsToDefenseCatalog() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"));
    }

    @Test
    void catalogPageRendersDefenseTableWithAggregatedValues() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of(createTwoPartDefense()));

        mockMvc.perform(get("/defenses"))
                .andExpect(status().isOk())
                .andExpect(view().name("defenses/list"))
                .andExpect(model().attribute("videoCount", 2))
                .andExpect(content().string(containsString("id=\"defenses-table\"")))
                .andExpect(content().string(containsString("Петренко Олена Іванівна")))
                .andExpect(content().string(containsString("Методи аналізу програмних систем")))
                .andExpect(content().string(containsString("Київський політехнічний інститут")))
                .andExpect(content().string(containsString("1 530")))
                .andExpect(content().string(containsString("href=\"/defenses/7\"")))
                .andExpect(content().string(containsString("id=\"defenseModal-7\"")))
                .andExpect(content().string(containsString("Сума переглядів")));
    }

    @Test
    void catalogPageRendersFilterAndControlPanels() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of());
        when(defenseCatalogService.findUniversities()).thenReturn(List.of("Львівська політехніка"));
        when(defenseCatalogService.findChannelNames()).thenReturn(List.of("КПІ ім. Ігоря Сікорського"));
        when(defenseCatalogService.findSpecialties()).thenReturn(List.of("121 Інженерія програмного забезпечення"));

        mockMvc.perform(get("/defenses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Панель керування")))
                .andExpect(content().string(containsString("action=\"/defenses/collect\"")))
                .andExpect(content().string(containsString("action=\"/defenses/refresh-statistics\"")))
                .andExpect(content().string(containsString("Оновити статистику")))
                .andExpect(content().string(containsString("Фільтри")))
                .andExpect(content().string(containsString("<option value=\"Львівська політехніка\">")))
                .andExpect(content().string(containsString("<option value=\"КПІ ім. Ігоря Сікорського\">")))
                .andExpect(content().string(containsString("<option value=\"121 Інженерія програмного забезпечення\">")))
                .andExpect(content().string(containsString("Сортувати за:")))
                .andExpect(content().string(containsString("Переглядами")));
    }

    @Test
    void emptyCatalogShowsHintInsteadOfTable() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of());

        mockMvc.perform(get("/defenses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Захистів не знайдено")))
                .andExpect(content().string(not(containsString("id=\"defenses-table\""))));
    }

    @Test
    void filterParametersArePassedToCatalogService() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of());

        mockMvc.perform(get("/defenses")
                        .param("councilOrUniversity", "КНУ")
                        .param("specialty", "121")
                        .param("publishedFrom", "2025-01-01")
                        .param("minViews", "100")
                        .param("sortField", "VIEWS")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("filterErrors"));

        ArgumentCaptor<VideoFilterCriteria> criteriaCaptor = ArgumentCaptor.forClass(VideoFilterCriteria.class);
        verify(defenseCatalogService).findDefenses(criteriaCaptor.capture());
        VideoFilterCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getCouncilOrUniversity()).isEqualTo("КНУ");
        assertThat(criteria.getSpecialty()).isEqualTo("121");
        assertThat(criteria.getPublishedFrom()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(criteria.getMinViews()).isEqualTo(100);
        assertThat(criteria.getSortField()).isEqualTo(SortField.VIEWS);
        assertThat(criteria.getSortDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void invalidFilterParametersShowUkrainianErrorsAndFallBackToFullCatalog() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of());

        mockMvc.perform(get("/defenses")
                        .param("channelName", "Канал")
                        .param("minViews", "500")
                        .param("maxViews", "10")
                        .param("sortField", "POPULARITY"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("filterErrors"))
                .andExpect(content().string(containsString("Фільтри не застосовано через помилки")))
                .andExpect(content().string(containsString("Мінімальна кількість переглядів не може перевищувати максимальну")))
                .andExpect(content().string(containsString("Обрано невідоме поле сортування")));

        ArgumentCaptor<VideoFilterCriteria> criteriaCaptor = ArgumentCaptor.forClass(VideoFilterCriteria.class);
        verify(defenseCatalogService).findDefenses(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().getChannelName()).isNull();
        assertThat(criteriaCaptor.getValue().getMinViews()).isNull();
    }

    @Test
    @DisplayName("TC-FR-07: сторінка захисту показує сумарні перегляди та статистику кожної частини")
    void defenseDetailsShowTotalsAndEveryPartSeparately() throws Exception {
        when(defenseCatalogService.findDefense(7L)).thenReturn(Optional.of(createTwoPartDefense()));

        mockMvc.perform(get("/defenses/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("defenses/detail"))
                .andExpect(content().string(containsString("Сумарні перегляди")))
                .andExpect(content().string(containsString("id=\"total-views\"")))
                .andExpect(content().string(containsString("1 530")))
                .andExpect(content().string(containsString("Частина 1")))
                .andExpect(content().string(containsString("Частина 2")))
                .andExpect(content().string(containsString("1 год 30 хв")))
                .andExpect(content().string(containsString("45 хв")))
                .andExpect(content().string(containsString("1 200")))
                .andExpect(content().string(containsString("330")))
                .andExpect(content().string(containsString("78,4%")))
                .andExpect(content().string(containsString("https://www.youtube.com/watch?v=part1VideoId")))
                .andExpect(content().string(containsString("https://www.youtube.com/watch?v=part2VideoId")));
    }

    @Test
    void missingDefenseRedirectsToCatalogWithError() throws Exception {
        when(defenseCatalogService.findDefense(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/defenses/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashError", "Захист дисертації з ідентифікатором 99 не знайдено"));
    }

    @Test
    void nonNumericDefenseIdRedirectsWithValidationMessage() throws Exception {
        mockMvc.perform(get("/defenses/abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashError", "Некоректний ідентифікатор захисту дисертації"));
    }

    @Test
    void nonPositiveDefenseIdIsRejectedByValidation() throws Exception {
        mockMvc.perform(get("/defenses/0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashError", "Некоректний ідентифікатор захисту дисертації"));

        verify(defenseCatalogService, never()).findDefense(any());
    }

    @Test
    void validSearchQueryCollectsVideosAndReportsResult() throws Exception {
        when(defenseCatalogService.collectDefenses("захист дисертації")).thenReturn(new CollectionResultDto(5, 3));

        mockMvc.perform(post("/defenses/collect").param("query", "  захист дисертації  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashSuccess",
                        "За запитом «захист дисертації» збережено відео: 5, сформовано захистів: 3"));
    }

    @Test
    void searchWithoutResultsShowsWarning() throws Exception {
        when(defenseCatalogService.collectDefenses("рідкісний запит")).thenReturn(new CollectionResultDto(0, 0));

        mockMvc.perform(post("/defenses/collect").param("query", "рідкісний запит"))
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashWarning", "За запитом «рідкісний запит» відео не знайдено"));
    }

    @Test
    void blankSearchQueryIsRejectedWithoutCallingYouTube() throws Exception {
        mockMvc.perform(post("/defenses/collect").param("query", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashError", containsString("Введіть пошуковий запит для YouTube")));

        verify(defenseCatalogService, never()).collectDefenses(anyString());
    }

    @Test
    void tooShortSearchQueryIsRejected() throws Exception {
        mockMvc.perform(post("/defenses/collect").param("query", "ab"))
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashError", "Пошуковий запит повинен містити від 3 до 200 символів"));

        verify(defenseCatalogService, never()).collectDefenses(anyString());
    }

    @Test
    void quotaExceededDuringSearchIsShownAsWarning() throws Exception {
        when(defenseCatalogService.collectDefenses(anyString()))
                .thenThrow(new QuotaExceededException("Перевищено добову квоту запитів YouTube Data API v3"));

        mockMvc.perform(post("/defenses/collect").param("query", "захист PhD"))
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashWarning", "Перевищено добову квоту запитів YouTube Data API v3"));
    }

    @Test
    void youTubeApiErrorDuringSearchIsShownAsError() throws Exception {
        when(defenseCatalogService.collectDefenses(anyString()))
                .thenThrow(new YouTubeApiException("Помилка з'єднання з сервером YouTube API"));

        mockMvc.perform(post("/defenses/collect").param("query", "захист PhD"))
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashError", containsString("Не вдалося виконати пошук на YouTube")));
    }

    @Test
    void refreshStatisticsReportsNumberOfUpdatedVideos() throws Exception {
        when(dataCollectorService.updateStatistics()).thenReturn(List.of(new DefenseVideo(), new DefenseVideo()));

        mockMvc.perform(post("/defenses/refresh-statistics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/defenses"))
                .andExpect(flash().attribute("flashSuccess", "Статистику оновлено для відео: 2"));
    }

    @Test
    void refreshStatisticsWithoutVideosShowsWarning() throws Exception {
        when(dataCollectorService.updateStatistics()).thenReturn(List.of());

        mockMvc.perform(post("/defenses/refresh-statistics"))
                .andExpect(flash().attribute("flashWarning", "Немає збережених відео для оновлення статистики"));
    }

    @Test
    void refreshStatisticsFailureIsShownAsError() throws Exception {
        when(dataCollectorService.updateStatistics()).thenThrow(new YouTubeApiException("Сервер недоступний"));

        mockMvc.perform(post("/defenses/refresh-statistics"))
                .andExpect(flash().attribute("flashError", "Не вдалося оновити статистику. Сервер недоступний"));
    }

    @Test
    void flashMessageIsRenderedOnCatalogPage() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of());

        mockMvc.perform(get("/defenses").flashAttr("flashSuccess", "Статистику оновлено для відео: 4"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-success")))
                .andExpect(content().string(containsString("Статистику оновлено для відео: 4")));
    }

    private DefenseAggregationDto createTwoPartDefense() {
        VideoPartDetailDto firstPart = VideoPartDetailDto.builder()
                .videoId(1L)
                .youtubeVideoId("part1VideoId")
                .partNumber(1)
                .title("Захист дисертації Петренко О. І. (частина 1)")
                .publishedAt(LocalDate.of(2025, 3, 12))
                .durationSeconds(5400)
                .formattedDuration("1 год 30 хв")
                .viewCount(1200)
                .likeCount(40)
                .commentCount(6)
                .channelName("КПІ ім. Ігоря Сікорського")
                .build();
        VideoPartDetailDto secondPart = VideoPartDetailDto.builder()
                .videoId(2L)
                .youtubeVideoId("part2VideoId")
                .partNumber(2)
                .title("Захист дисертації Петренко О. І. (частина 2)")
                .publishedAt(LocalDate.of(2025, 3, 12))
                .durationSeconds(2700)
                .formattedDuration("45 хв")
                .viewCount(330)
                .likeCount(12)
                .commentCount(2)
                .channelName("КПІ ім. Ігоря Сікорського")
                .build();

        return DefenseAggregationDto.builder()
                .dissertationId(7L)
                .authorFullName("Петренко Олена Іванівна")
                .topic("Методи аналізу програмних систем")
                .specialty("121 Інженерія програмного забезпечення")
                .councilName("ДФ 26.002.001")
                .universityName("Київський політехнічний інститут")
                .totalPartsCount(2)
                .totalViewCount(1530)
                .totalLikeCount(52)
                .totalCommentCount(8)
                .totalDurationSeconds(8100)
                .formattedTotalDuration("2 год 15 хв")
                .parts(List.of(firstPart, secondPart))
                .build();
    }
}

