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
public class DescriptiveStatisticsDto {

    private double average;
    private double median;
    private long count;
    private double sum;
    private double min;
    private double max;
}

