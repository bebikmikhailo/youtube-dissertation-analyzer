package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.dissertation.client.youtube.YouTubeDemoDataFactory;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.UniversityPresentationProfile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CouncilComparisonTest {

    private CouncilPresentationAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        DissertationMetadataParser parser = new DissertationMetadataParser();
        analyzer = new CouncilPresentationAnalyzer(parser);
    }

    @Test
    @DisplayName("FR-14: Формування порівняльного звіту для провідних університетів України")
    void shouldGenerateTopUniversitiesComparisonWithDistinctProfiles() {
        PresentationComparisonReport report = analyzer.generateTopUniversitiesComparison();

        boolean isReportNotNull = report != null;
        assertThat(isReportNotNull).isTrue();

        List<UniversityPresentationProfile> profiles = report.getProfiles();
        boolean hasFourUniversities = profiles.size() == 4;
        assertThat(hasFourUniversities).isTrue();

        Optional<UniversityPresentationProfile> nubipOptional = profiles.stream()
                .filter(p -> p.getUniversityName().contains("НУБіП"))
                .findFirst();
        boolean isNubipPresent = nubipOptional.isPresent();
        assertThat(isNubipPresent).isTrue();

        UniversityPresentationProfile nubip = nubipOptional.get();
        boolean isNubipMultiPartDetected = nubip.isMultiPartPracticeDetected();
        boolean isNubipPartPercentagePositive = nubip.getTitleHasPartNumberPercentage() > 0.0;
        assertThat(isNubipMultiPartDetected).isTrue();
        assertThat(isNubipPartPercentagePositive).isTrue();

        Optional<UniversityPresentationProfile> kpiOptional = profiles.stream()
                .filter(p -> p.getUniversityName().contains("КПІ"))
                .findFirst();
        boolean isKpiPresent = kpiOptional.isPresent();
        assertThat(isKpiPresent).isTrue();

        UniversityPresentationProfile kpi = kpiOptional.get();
        boolean isKpiDegreeInTitle = kpi.getTitleHasDegreePercentage() == 100.0;
        boolean isKpiStructuredLabelsInDescription = kpi.getDescriptionHasStructuredLabelsPercentage() == 100.0;
        assertThat(isKpiDegreeInTitle).isTrue();
        assertThat(isKpiStructuredLabelsInDescription).isTrue();

        Optional<UniversityPresentationProfile> knuOptional = profiles.stream()
                .filter(p -> p.getUniversityName().contains("КНУ"))
                .findFirst();
        boolean isKnuPresent = knuOptional.isPresent();
        assertThat(isKnuPresent).isTrue();

        UniversityPresentationProfile knu = knuOptional.get();
        boolean isKnuTopicInTitle = knu.getTitleHasTopicPercentage() == 100.0;
        assertThat(isKnuTopicInTitle).isTrue();

        Optional<UniversityPresentationProfile> lpOptional = profiles.stream()
                .filter(p -> p.getUniversityName().contains("Львівська політехніка"))
                .findFirst();
        boolean isLpPresent = lpOptional.isPresent();
        assertThat(isLpPresent).isTrue();

        UniversityPresentationProfile lp = lpOptional.get();
        boolean isLpTopicInTitle = lp.getTitleHasTopicPercentage() == 100.0;
        assertThat(isLpTopicInTitle).isTrue();
    }

    @Test
    @DisplayName("FR-14: Перевірка наявності спільних ознак, відмінностей та аналітичного висновку")
    void shouldContainCommonFeaturesDifferencesAndAnalyticalConclusion() {
        PresentationComparisonReport report = analyzer.generateTopUniversitiesComparison();

        List<String> commonFeatures = report.getCommonFeatures();
        boolean hasCommonFeatures = commonFeatures != null && !commonFeatures.isEmpty();
        assertThat(hasCommonFeatures).isTrue();

        List<String> differences = report.getDifferences();
        boolean hasDifferences = differences != null && !differences.isEmpty();
        assertThat(hasDifferences).isTrue();

        boolean mentionsMultiPart = differences.stream().anyMatch(diff -> diff.contains("частини"));
        boolean mentionsTitleDifferences = differences.stream().anyMatch(diff -> diff.contains("Структура заголовка"));
        assertThat(mentionsMultiPart).isTrue();
        assertThat(mentionsTitleDifferences).isTrue();

        String conclusion = report.getAnalyticalConclusion();
        boolean isConclusionInformative = conclusion != null && conclusion.length() > 100;
        boolean mentionsTopUniversities = conclusion.contains("КПІ") && conclusion.contains("НУБіП")
                && conclusion.contains("КНУ") && conclusion.contains("Львівська політехніка");

        assertThat(isConclusionInformative).isTrue();
        assertThat(mentionsTopUniversities).isTrue();
    }

    @Test
    @DisplayName("FR-14: Коректна обробка порожнього списку відео")
    void shouldHandleEmptyListGracefully() {
        PresentationComparisonReport report = analyzer.analyzeVideoDtos(List.of());

        boolean isReportNotNull = report != null;
        boolean areProfilesEmpty = report.getProfiles().isEmpty();
        boolean hasSafeConclusion = report.getAnalyticalConclusion() != null;

        assertThat(isReportNotNull).isTrue();
        assertThat(areProfilesEmpty).isTrue();
        assertThat(hasSafeConclusion).isTrue();
    }
}

