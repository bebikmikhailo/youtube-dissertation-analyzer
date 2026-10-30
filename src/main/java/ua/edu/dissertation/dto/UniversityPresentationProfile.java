package ua.edu.dissertation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityPresentationProfile {

    private String universityName;
    private int totalVideosAnalyzed;
    private double titleHasAuthorPercentage;
    private double titleHasTopicPercentage;
    private double titleHasDegreePercentage;
    private double titleHasPartNumberPercentage;
    private double descriptionHasStructuredLabelsPercentage;
    private double descriptionHasCouncilCodePercentage;
    private double descriptionHasSpecialtyCodePercentage;
    private boolean multiPartPracticeDetected;
    private String styleSummary;
}

