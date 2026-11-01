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
public class CorrelationAnalysisDto {

    private CorrelationResultDto durationCorrelation;
    private CorrelationResultDto ageDaysCorrelation;
    private CorrelationResultDto subtitlesCorrelation;
    private List<CorrelationResultDto> allCorrelations;
    private int totalVideosAnalyzed;
}

