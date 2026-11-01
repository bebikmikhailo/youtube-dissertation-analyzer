package ua.edu.dissertation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.VideoPartDetailDto;
import ua.edu.dissertation.service.DataCollectorService;
import ua.edu.dissertation.service.DefenseCatalogService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DefenseController.class)
class UiLocalizationAndLayoutTest {

    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}]");
    private static final String EM_DASH = "\u2014";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DefenseCatalogService defenseCatalogService;

    @MockitoBean
    private DataCollectorService dataCollectorService;

    @Test
    void catalogPageIsUkrainianAndResponsive() throws Exception {
        when(defenseCatalogService.findDefenses(any())).thenReturn(List.of(createSinglePartDefense()));

        String html = renderPage("/defenses");

        assertResponsiveUkrainianLayout(html);
        assertThat(html).contains(
                "Каталог захистів",
                "Усі захисти",
                "Найпопулярніші",
                "Здобувач і тема",
                "Спеціальність",
                "Перегляди",
                "Вподобання",
                "Коментарі",
                "Застосувати",
                "Скинути фільтри",
                "table-responsive");
    }

    @Test
    void detailPageIsUkrainianAndResponsive() throws Exception {
        when(defenseCatalogService.findDefense(3L)).thenReturn(Optional.of(createSinglePartDefense()));

        String html = renderPage("/defenses/3");

        assertResponsiveUkrainianLayout(html);
        assertThat(html).contains(
                "Сумарна статистика захисту",
                "Статистика за частинами захисту",
                "Частка переглядів",
                "Разом",
                "До каталогу захистів",
                "table-responsive");
    }

    private String renderPage(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void assertResponsiveUkrainianLayout(String html) {
        assertThat(html).contains("<html lang=\"uk\"");
        assertThat(html).contains("name=\"viewport\" content=\"width=device-width, initial-scale=1\"");
        assertThat(html).contains("bootstrap@5.3.3/dist/css/bootstrap.min.css");
        assertThat(html).contains("bootstrap-icons");
        assertThat(html).contains("class=\"bi bi-search\"");
        assertThat(html).doesNotContain(EM_DASH);
        assertThat(EMOJI_PATTERN.matcher(html).find()).isFalse();
    }

    private DefenseAggregationDto createSinglePartDefense() {
        VideoPartDetailDto part = VideoPartDetailDto.builder()
                .videoId(10L)
                .youtubeVideoId("singleVideo1")
                .partNumber(1)
                .title("Разовий захист PhD Коваленко Андрій Петрович")
                .publishedAt(LocalDate.of(2025, 5, 20))
                .durationSeconds(3600)
                .formattedDuration("1 год")
                .viewCount(250)
                .channelName("Національний університет біоресурсів і природокористування")
                .build();

        return DefenseAggregationDto.builder()
                .dissertationId(3L)
                .authorFullName("Коваленко Андрій Петрович")
                .topic("Агроекологічні моделі прогнозування врожайності")
                .specialty("201 Агрономія")
                .universityName("НУБіП України")
                .totalPartsCount(1)
                .totalViewCount(250)
                .totalDurationSeconds(3600)
                .formattedTotalDuration("1 год")
                .parts(List.of(part))
                .build();
    }
}

