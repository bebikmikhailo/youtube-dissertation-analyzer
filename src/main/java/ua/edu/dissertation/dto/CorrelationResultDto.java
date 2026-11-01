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
public class CorrelationResultDto {

    private String factorName;
    private double coefficient;
    private String strength;
    private String interpretation;
    private int sampleSize;
    private boolean isPositive;
}

