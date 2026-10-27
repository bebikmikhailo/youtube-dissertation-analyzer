package ua.edu.dissertation.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class YouTubeApiConfig {

    private final String apiKey;

    public YouTubeApiConfig(@Value("${youtube.api.key:${YOUTUBE_API_KEY:demo-key}}") String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isDemoMode() {
        boolean isMissing = apiKey == null || apiKey.isBlank();
        boolean isDemoKey = "demo-key".equalsIgnoreCase(apiKey);
        return isMissing || isDemoKey;
    }
}
