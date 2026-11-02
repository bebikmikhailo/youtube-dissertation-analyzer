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
public class DashboardSummaryDto {

    private int totalDefenses;
    private long totalViews;
    private double averageViews;
    private double medianViews;
    private long multiPartCount;
    private int totalVideosCount;
}

