package ua.edu.dissertation.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_NFR_06_SecurityConfigTest {

    @Autowired
    private YouTubeApiConfig youTubeApiConfig;

    @Test
    @DisplayName("NFR-06: Завантаження конфігурації YouTube API з контексту Spring")
    void shouldLoadApiKeyConfigurationFromContext() {
        boolean isConfigPresent = youTubeApiConfig != null;
        assertThat(isConfigPresent).isTrue();

        boolean hasApiKey = youTubeApiConfig.getApiKey() != null && !youTubeApiConfig.getApiKey().isBlank();
        assertThat(hasApiKey).isTrue();
    }

    @Test
    @DisplayName("NFR-06: Визначення демо-режиму за замовчуванням")
    void shouldRecognizeDemoModeByDefault() {
        boolean isDemoMode = youTubeApiConfig.isDemoMode();
        assertThat(isDemoMode).isTrue();
    }

    @Test
    @DisplayName("NFR-06: Коректна обробка персонального API-ключа")
    void shouldAcceptCustomApiKeyAndDisableDemoMode() {
        String testKey = "custom-test-youtube-key";
        YouTubeApiConfig customConfig = new YouTubeApiConfig(testKey);

        boolean hasExpectedKey = testKey.equals(customConfig.getApiKey());
        boolean isDemoDisabled = !customConfig.isDemoMode();

        assertThat(hasExpectedKey).isTrue();
        assertThat(isDemoDisabled).isTrue();
    }

    @Test
    @DisplayName("NFR-06: Відсутність хардкоду секретного ключа в application.yml")
    void shouldEnsureApplicationYmlDoesNotHardcodeSecretKey() throws IOException {
        Path ymlPath = Path.of("src/main/resources/application.yml");
        boolean ymlExists = Files.exists(ymlPath);
        assertThat(ymlExists).isTrue();

        String content = Files.readString(ymlPath);
        boolean usesEnvPlaceholder = content.contains("${YOUTUBE_API_KEY");
        boolean hasHardcodedGoogleToken = content.contains("AIzaSy");

        assertThat(usesEnvPlaceholder).isTrue();
        assertThat(hasHardcodedGoogleToken).isFalse();
    }

    @Test
    @DisplayName("NFR-06: Наявність .env у файлі .gitignore")
    void shouldVerifyGitignoreProtectsEnvironmentFiles() throws IOException {
        Path gitignorePath = Path.of(".gitignore");
        boolean gitignoreExists = Files.exists(gitignorePath);
        assertThat(gitignoreExists).isTrue();

        String content = Files.readString(gitignorePath);
        boolean containsDotEnv = content.lines().anyMatch(line -> line.trim().equals(".env"));

        assertThat(containsDotEnv).isTrue();
    }
}
