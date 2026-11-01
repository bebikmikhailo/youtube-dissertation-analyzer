package ua.edu.dissertation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewDynamicsReportDto {

    private DynamicsInterval interval;
    private List<ViewDynamicsPointDto> points;
    private long initialViews;
    private long latestViews;
    private long totalGrowth;
    private int totalSnapshots;
}

