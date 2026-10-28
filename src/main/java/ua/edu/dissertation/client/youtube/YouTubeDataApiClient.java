package ua.edu.dissertation.client.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.client.youtube.exception.QuotaExceededException;
import ua.edu.dissertation.client.youtube.exception.YouTubeApiException;
import ua.edu.dissertation.config.YouTubeApiConfig;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YouTubeDataApiClient {

    private static final String DEFAULT_BASE_URL = "https://www.googleapis.com/youtube/v3";
    private static final int BATCH_SIZE = 50;

    private final RestTemplate restTemplate;
    private final YouTubeApiConfig apiConfig;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    @Autowired
    public YouTubeDataApiClient(YouTubeApiConfig apiConfig, ObjectMapper objectMapper) {
        this(new RestTemplate(), apiConfig, objectMapper, DEFAULT_BASE_URL);
    }

    public YouTubeDataApiClient(RestTemplate restTemplate, YouTubeApiConfig apiConfig, ObjectMapper objectMapper) {
        this(restTemplate, apiConfig, objectMapper, DEFAULT_BASE_URL);
    }

    public YouTubeDataApiClient(RestTemplate restTemplate, YouTubeApiConfig apiConfig, ObjectMapper objectMapper, String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public List<YouTubeVideoDto> searchVideos(String query) {
        return searchVideos(query, true);
    }

    public List<YouTubeVideoDto> searchVideos(String query, boolean fallbackToDemoOnQuota) {
        boolean isDemoActive = apiConfig.isDemoMode();
        if (isDemoActive) {
            log.info("Використовується демо-режим клієнта YouTube API для пошуку: {}", query);
            return filterDemoVideosByQuery(query);
        }

        try {
            URI searchUri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/search")
                    .queryParam("part", "snippet")
                    .queryParam("type", "video")
                    .queryParam("maxResults", 50)
                    .queryParam("q", query)
                    .queryParam("key", apiConfig.getApiKey())
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(searchUri, String.class);
            List<String> videoIds = extractVideoIdsFromSearchResponse(response.getBody());

            boolean hasNoResults = videoIds.isEmpty();
            if (hasNoResults) {
                return List.of();
            }

            return getVideosByIds(videoIds, fallbackToDemoOnQuota);
        } catch (HttpStatusCodeException exception) {
            boolean isQuotaExceeded = isQuotaExceededResponse(exception);
            if (isQuotaExceeded && fallbackToDemoOnQuota) {
                log.warn("Вичерпано квоту YouTube API під час пошуку. Перемикання на демо-дані.");
                return filterDemoVideosByQuery(query);
            }
            handleHttpStatusCodeException(exception);
            return List.of();
        } catch (ResourceAccessException exception) {
            log.error("Помилка мережевого з'єднання з YouTube API: {}", exception.getMessage());
            throw new YouTubeApiException("Помилка з'єднання з сервером YouTube API (таймаут або відсутність зв'язку)", exception);
        } catch (YouTubeApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Непередбачена помилка під час пошуку відео: {}", exception.getMessage());
            throw new YouTubeApiException("Непередбачена помилка клієнта YouTube API", exception);
        }
    }

    public List<YouTubeVideoDto> getVideosByIds(List<String> videoIds) {
        return getVideosByIds(videoIds, false);
    }

    public List<YouTubeVideoDto> getVideosByIds(List<String> videoIds, boolean fallbackToDemoOnQuota) {
        boolean isInputEmpty = videoIds == null || videoIds.isEmpty();
        if (isInputEmpty) {
            return List.of();
        }

        boolean isDemoActive = apiConfig.isDemoMode();
        if (isDemoActive) {
            log.info("Використовується демо-режим клієнта YouTube API для отримання деталей відео.");
            return filterDemoVideosByIds(videoIds);
        }

        List<List<String>> batches = partitionList(videoIds, BATCH_SIZE);
        List<YouTubeVideoDto> collectedVideos = new ArrayList<>();

        for (List<String> batch : batches) {
            try {
                String joinedIds = String.join(",", batch);
                URI videosUri = UriComponentsBuilder.fromUriString(baseUrl)
                        .path("/videos")
                        .queryParam("part", "snippet,contentDetails,statistics")
                        .queryParam("id", joinedIds)
                        .queryParam("key", apiConfig.getApiKey())
                        .build()
                        .toUri();

                ResponseEntity<String> response = restTemplate.getForEntity(videosUri, String.class);
                List<YouTubeVideoDto> parsedBatch = parseVideosResponse(response.getBody());
                collectedVideos.addAll(parsedBatch);
            } catch (HttpStatusCodeException exception) {
                boolean isQuotaExceeded = isQuotaExceededResponse(exception);
                if (isQuotaExceeded && fallbackToDemoOnQuota) {
                    log.warn("Вичерпано квоту під час пакетного запиту. Перемикання на демо-відповіді.");
                    return filterDemoVideosByIds(videoIds);
                }
                handleHttpStatusCodeException(exception);
            } catch (ResourceAccessException exception) {
                log.error("Помилка таймауту запиту статистики відео: {}", exception.getMessage());
                throw new YouTubeApiException("Помилка з'єднання з сервером YouTube API (таймаут або мережева помилка)", exception);
            } catch (YouTubeApiException exception) {
                throw exception;
            } catch (Exception exception) {
                log.error("Помилка обробки пакетного запиту відео: {}", exception.getMessage());
                throw new YouTubeApiException("Непередбачена помилка при отриманні списку відео", exception);
            }
        }

        return collectedVideos;
    }

    private void handleHttpStatusCodeException(HttpStatusCodeException exception) {
        String responseBody = exception.getResponseBodyAsString();
        boolean isQuotaExceeded = isQuotaExceededResponse(exception);

        if (isQuotaExceeded) {
            log.warn("Перевищено квоту YouTube Data API (quotaExceeded).");
            throw new QuotaExceededException("Перевищено добову квоту запитів YouTube Data API v3 (quotaExceeded). Спробуйте пізніше або активуйте демо-режим.", exception);
        }

        boolean isKeyInvalid = responseBody.contains("keyInvalid") || responseBody.contains("API key not valid");
        if (isKeyInvalid) {
            log.error("Недійсний API-ключ YouTube.");
            throw new YouTubeApiException("Недійсний ключ YouTube Data API v3. Перевірте параметр YOUTUBE_API_KEY у конфігурації.", exception);
        }

        log.error("HTTP помилка YouTube API: status={}", exception.getStatusCode());
        throw new YouTubeApiException("Помилка виконання HTTP-запиту до YouTube Data API: " + exception.getStatusCode(), exception);
    }

    private boolean isQuotaExceededResponse(HttpStatusCodeException exception) {
        String responseBody = exception.getResponseBodyAsString();
        boolean hasQuotaWord = responseBody.contains("quotaExceeded");
        boolean hasQuotaMessage = responseBody.contains("exceeded your quota");
        return hasQuotaWord || hasQuotaMessage;
    }

    private List<String> extractVideoIdsFromSearchResponse(String json) {
        List<String> ids = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return ids;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    String videoId = item.path("id").path("videoId").asText();
                    boolean hasValidId = videoId != null && !videoId.isBlank();
                    if (hasValidId) {
                        ids.add(videoId);
                    }
                }
            }
        } catch (Exception exception) {
            log.error("Помилка парсингу результатів пошуку YouTube API: {}", exception.getMessage());
        }

        return ids;
    }

    private List<YouTubeVideoDto> parseVideosResponse(String json) {
        List<YouTubeVideoDto> list = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return list;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    list.add(mapJsonToVideoDto(item));
                }
            }
        } catch (Exception exception) {
            log.error("Помилка парсингу елементів відео: {}", exception.getMessage());
            throw new YouTubeApiException("Помилка розбору відповіді YouTube Data API", exception);
        }

        return list;
    }

    private YouTubeVideoDto mapJsonToVideoDto(JsonNode itemNode) {
        String id = itemNode.path("id").asText();
        JsonNode snippet = itemNode.path("snippet");
        JsonNode contentDetails = itemNode.path("contentDetails");
        JsonNode statistics = itemNode.path("statistics");

        String title = snippet.path("title").asText("");
        String description = snippet.path("description").asText("");
        String channelId = snippet.path("channelId").asText("");
        String channelTitle = snippet.path("channelTitle").asText("");
        LocalDate publishedAt = parsePublishedAt(snippet.path("publishedAt").asText(""));

        int durationSeconds = parseIsoDuration(contentDetails.path("duration").asText(""));
        boolean hasSubtitles = "true".equalsIgnoreCase(contentDetails.path("caption").asText("false"));

        int viewCount = parseIntSafely(statistics.path("viewCount").asText("0"));
        int likeCount = parseIntSafely(statistics.path("likeCount").asText("0"));
        int commentCount = parseIntSafely(statistics.path("commentCount").asText("0"));

        return YouTubeVideoDto.builder()
                .id(id)
                .title(title)
                .description(description)
                .channelId(channelId)
                .channelTitle(channelTitle)
                .publishedAt(publishedAt)
                .durationSeconds(durationSeconds)
                .hasSubtitles(hasSubtitles)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .build();
    }

    private LocalDate parsePublishedAt(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return LocalDate.now();
        }
        try {
            return OffsetDateTime.parse(isoString).toLocalDate();
        } catch (Exception ignored) {
            return LocalDate.now();
        }
    }

    private int parseIsoDuration(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) {
            return 0;
        }
        try {
            return (int) Duration.parse(isoDuration).toSeconds();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int parseIntSafely(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            int end = Math.min(i + size, list.size());
            partitions.add(list.subList(i, end));
        }
        return partitions;
    }

    private List<YouTubeVideoDto> filterDemoVideosByQuery(String query) {
        List<YouTubeVideoDto> allDemo = YouTubeDemoDataFactory.createDemoVideos();
        boolean isQueryEmpty = query == null || query.isBlank();
        if (isQueryEmpty) {
            return allDemo;
        }

        String lowerQuery = query.toLowerCase();
        List<YouTubeVideoDto> filtered = allDemo.stream()
                .filter(v -> v.getTitle().toLowerCase().contains(lowerQuery)
                        || v.getDescription().toLowerCase().contains(lowerQuery)
                        || v.getChannelTitle().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());

        boolean hasMatches = !filtered.isEmpty();
        return hasMatches ? filtered : allDemo;
    }

    private List<YouTubeVideoDto> filterDemoVideosByIds(List<String> videoIds) {
        List<YouTubeVideoDto> allDemo = YouTubeDemoDataFactory.createDemoVideos();
        List<YouTubeVideoDto> matched = allDemo.stream()
                .filter(v -> videoIds.contains(v.getId()))
                .collect(Collectors.toList());

        boolean hasMatches = !matched.isEmpty();
        return hasMatches ? matched : allDemo;
    }
}
