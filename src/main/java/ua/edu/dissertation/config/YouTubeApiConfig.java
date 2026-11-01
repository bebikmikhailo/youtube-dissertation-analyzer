package ua.edu.dissertation.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class YouTubeApiConfig {

    private final String apiKey;

    @Autowired
    public YouTubeApiConfig(@Value("${youtube.api.key:}") String propertyKey,
                            @Value("${YOUTUBE_API_KEY:}") String envKey) {
        this.apiKey = resolveApiKey(propertyKey, envKey);
    }

    public YouTubeApiConfig(String apiKey) {
        this.apiKey = resolveApiKey(apiKey, null);
    }

    public boolean isDemoMode() {
        boolean isMissing = apiKey == null || apiKey.isBlank();
        boolean isDemoKey = "demo-key".equalsIgnoreCase(apiKey);
        return isMissing || isDemoKey;
    }

    private String resolveApiKey(String propertyKey, String envKey) {
        boolean hasProperty = propertyKey != null && !propertyKey.isBlank();
        if (hasProperty) {
            return propertyKey.trim();
        }

        boolean hasEnv = envKey != null && !envKey.isBlank();
        if (hasEnv) {
            return envKey.trim();
        }

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String dotKey = dotenv.get("YOUTUBE_API_KEY");
        boolean hasDotKey = dotKey != null && !dotKey.isBlank();
        if (hasDotKey) {
            return dotKey.trim();
        }

        return "demo-key";
    }
}
