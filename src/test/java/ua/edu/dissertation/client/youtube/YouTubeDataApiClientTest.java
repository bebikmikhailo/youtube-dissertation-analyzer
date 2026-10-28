package ua.edu.dissertation.client.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.client.youtube.exception.QuotaExceededException;
import ua.edu.dissertation.client.youtube.exception.YouTubeApiException;
import ua.edu.dissertation.config.YouTubeApiConfig;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YouTubeDataApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private YouTubeApiConfig activeApiConfig;
    private YouTubeDataApiClient liveClient;

    private static final String TEST_BASE_URL = "https://www.googleapis.com/youtube/v3";
    private static final String TEST_KEY = "valid-test-key-12345";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
        activeApiConfig = new YouTubeApiConfig(TEST_KEY);
        liveClient = new YouTubeDataApiClient(restTemplate, activeApiConfig, objectMapper, TEST_BASE_URL);
    }

    @Test
    @DisplayName("Симуляція коду 200: успішне отримання та парсинг статистики відео")
    void shouldSuccessfullyFetchVideosOnHttp200() {
        String mockResponseJson = """
                {
                  "items": [
                    {
                      "id": "vid_kpi_01",
                      "snippet": {
                        "title": "Захист дисертації КПІ 121",
                        "description": "Опис захисту дисертації в КПІ",
                        "publishedAt": "2024-01-15T12:00:00Z",
                        "channelId": "UC_KPI_01",
                        "channelTitle": "КПІмедіа"
                      },
                      "contentDetails": {
                        "duration": "PT1H15M",
                        "caption": "true"
                      },
                      "statistics": {
                        "viewCount": "540",
                        "likeCount": "42",
                        "commentCount": "7"
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(TEST_BASE_URL + "/videos?part=snippet,contentDetails,statistics&id=vid_kpi_01&key=" + TEST_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        List<YouTubeVideoDto> result = liveClient.getVideosByIds(List.of("vid_kpi_01"), false);
        mockServer.verify();

        boolean hasSingleVideo = result.size() == 1;
        assertThat(hasSingleVideo).isTrue();

        YouTubeVideoDto video = result.get(0);
        boolean isIdMatching = "vid_kpi_01".equals(video.getId());
        boolean isDurationCorrect = video.getDurationSeconds() == 4500;
        boolean hasSubtitles = video.isHasSubtitles();
        boolean isViewCountCorrect = video.getViewCount() == 540;
        boolean isLikeCountCorrect = video.getLikeCount() == 42;

        assertThat(isIdMatching).isTrue();
        assertThat(isDurationCorrect).isTrue();
        assertThat(hasSubtitles).isTrue();
        assertThat(isViewCountCorrect).isTrue();
        assertThat(isLikeCountCorrect).isTrue();
    }

    @Test
    @DisplayName("Симуляція коду 403 (квота): генерація QuotaExceededException при вичерпанні ліміту")
    void shouldThrowQuotaExceededExceptionOnHttp403WithQuotaExceeded() {
        String quotaErrorJson = """
                {
                  "error": {
                    "code": 403,
                    "message": "The request cannot be completed because you have exceeded your quota.",
                    "errors": [
                      {
                        "domain": "youtube.quota",
                        "reason": "quotaExceeded",
                        "message": "The request cannot be completed because you have exceeded your quota."
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(TEST_BASE_URL + "/videos?part=snippet,contentDetails,statistics&id=vid_test&key=" + TEST_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(quotaErrorJson));

        assertThatThrownBy(() -> liveClient.getVideosByIds(List.of("vid_test"), false))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("quotaExceeded");

        mockServer.verify();
    }

    @Test
    @DisplayName("Симуляція коду 403 (квота): перемикання на демо-відповіді при увімкненому fallback")
    void shouldReturnDemoVideosWhenQuotaExceededAndFallbackEnabled() {
        String quotaErrorJson = """
                {
                  "error": {
                    "code": 403,
                    "message": "quotaExceeded"
                  }
                }
                """;

        mockServer.expect(requestTo(TEST_BASE_URL + "/videos?part=snippet,contentDetails,statistics&id=nubip_labenko_p1&key=" + TEST_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(quotaErrorJson));

        List<YouTubeVideoDto> fallbackVideos = liveClient.getVideosByIds(List.of("nubip_labenko_p1"), true);
        mockServer.verify();

        boolean hasFallbackVideos = !fallbackVideos.isEmpty();
        assertThat(hasFallbackVideos).isTrue();

        boolean containsDemoVideo = fallbackVideos.stream()
                .anyMatch(v -> "nubip_labenko_p1".equals(v.getId()));
        assertThat(containsDemoVideo).isTrue();
    }

    @Test
    @DisplayName("NFR-02: Пакетне запитування статистики відео частинами до 50 id (batching)")
    void shouldExecuteBatchingRequestsInChunksOfFifty() {
        List<String> seventyFiveIds = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            seventyFiveIds.add("video_id_" + i);
        }

        List<String> firstBatchIds = seventyFiveIds.subList(0, 50);
        List<String> secondBatchIds = seventyFiveIds.subList(50, 75);

        String firstBatchParam = String.join(",", firstBatchIds);
        String secondBatchParam = String.join(",", secondBatchIds);

        String firstBatchResponse = buildMockBatchResponse(firstBatchIds);
        String secondBatchResponse = buildMockBatchResponse(secondBatchIds);

        mockServer.expect(requestTo(TEST_BASE_URL + "/videos?part=snippet,contentDetails,statistics&id=" + firstBatchParam + "&key=" + TEST_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(firstBatchResponse, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(TEST_BASE_URL + "/videos?part=snippet,contentDetails,statistics&id=" + secondBatchParam + "&key=" + TEST_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(secondBatchResponse, MediaType.APPLICATION_JSON));

        List<YouTubeVideoDto> allRetrieved = liveClient.getVideosByIds(seventyFiveIds, false);
        mockServer.verify();

        boolean hasAllSeventyFiveVideos = allRetrieved.size() == 75;
        assertThat(hasAllSeventyFiveVideos).isTrue();
    }

    @Test
    @DisplayName("Робота клієнта в демо-режимі без виконання зовнішніх HTTP-запитів")
    void shouldReturnDemoVideosWhenInDemoModeWithoutHttpCalls() {
        YouTubeApiConfig demoConfig = new YouTubeApiConfig("demo-key");
        YouTubeDataApiClient demoClient = new YouTubeDataApiClient(restTemplate, demoConfig, objectMapper, TEST_BASE_URL);

        List<YouTubeVideoDto> searchResult = demoClient.searchVideos("КПІ");
        mockServer.verify();

        boolean isSearchResultNotEmpty = !searchResult.isEmpty();
        assertThat(isSearchResultNotEmpty).isTrue();

        boolean containsKpi = searchResult.stream()
                .anyMatch(v -> v.getTitle().contains("КПІ") || v.getDescription().contains("КПІ") || v.getChannelTitle().contains("КПІ"));
        assertThat(containsKpi).isTrue();

        List<YouTubeVideoDto> allDemo = YouTubeDemoDataFactory.createDemoVideos();
        boolean containsExpectedUniversities = allDemo.stream()
                .anyMatch(v -> v.getChannelTitle().contains("НУБіП") || v.getChannelTitle().contains("Львівська політехніка"));
        assertThat(containsExpectedUniversities).isTrue();
    }

    @Test
    @DisplayName("Обробка мережевого таймауту із генерацією YouTubeApiException")
    void shouldHandleNetworkTimeoutExceptionGracefully() {
        mockServer.expect(requestTo(TEST_BASE_URL + "/videos?part=snippet,contentDetails,statistics&id=timeout_id&key=" + TEST_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Connection timed out")));

        assertThatThrownBy(() -> liveClient.getVideosByIds(List.of("timeout_id"), false))
                .isInstanceOf(YouTubeApiException.class)
                .hasMessageContaining("таймаут");

        mockServer.verify();
    }

    private String buildMockBatchResponse(List<String> ids) {
        StringBuilder itemsBuilder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            itemsBuilder.append(String.format("""
                    {
                      "id": "%s",
                      "snippet": {
                        "title": "Відео %s",
                        "description": "Опис %s",
                        "publishedAt": "2024-01-01T00:00:00Z",
                        "channelId": "UC_TEST",
                        "channelTitle": "Тестовий канал"
                      },
                      "contentDetails": {
                        "duration": "PT30M",
                        "caption": "false"
                      },
                      "statistics": {
                        "viewCount": "100",
                        "likeCount": "10",
                        "commentCount": "2"
                      }
                    }
                    """, id, id, id));
            if (i < ids.size() - 1) {
                itemsBuilder.append(",");
            }
        }
        return "{\"items\": [" + itemsBuilder + "]}";
    }
}
