package ua.edu.dissertation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationComparisonReport {

    @Builder.Default
    private List<UniversityPresentationProfile> profiles = new ArrayList<>();

    @Builder.Default
    private List<String> commonFeatures = new ArrayList<>();

    @Builder.Default
    private List<String> differences = new ArrayList<>();

    private String analyticalConclusion;
}

