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
public class VideoMetricStatisticsDto {

    private DescriptiveStatisticsDto views;
    private DescriptiveStatisticsDto likes;
    private DescriptiveStatisticsDto comments;
    private int sampleSize;
}

