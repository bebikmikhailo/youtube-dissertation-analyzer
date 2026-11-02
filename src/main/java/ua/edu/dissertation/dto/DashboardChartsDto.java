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
public class DashboardChartsDto {

    @Builder.Default
    private List<String> channelLabels = new ArrayList<>();

    @Builder.Default
    private List<Long> channelViews = new ArrayList<>();

    @Builder.Default
    private List<Integer> channelVideoCounts = new ArrayList<>();

    @Builder.Default
    private List<String> specialtyLabels = new ArrayList<>();

    @Builder.Default
    private List<Integer> specialtyCounts = new ArrayList<>();

    @Builder.Default
    private List<Long> specialtyViews = new ArrayList<>();

    @Builder.Default
    private List<String> dynamicsLabels = new ArrayList<>();

    @Builder.Default
    private List<Long> dynamicsViews = new ArrayList<>();

    @Builder.Default
    private List<Long> dynamicsGrowths = new ArrayList<>();

    @Builder.Default
    private List<String> correlationLabels = new ArrayList<>();

    @Builder.Default
    private List<Double> correlationCoefficients = new ArrayList<>();

    @Builder.Default
    private List<String> correlationStrengths = new ArrayList<>();
}

