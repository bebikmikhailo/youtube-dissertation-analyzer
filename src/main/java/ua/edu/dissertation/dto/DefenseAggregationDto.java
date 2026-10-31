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
public class DefenseAggregationDto {

    private Long dissertationId;
    private String authorFullName;
    private String topic;
    private String specialty;
    private String councilName;
    private String universityName;
    private int totalPartsCount;
    private int totalViewCount;
    private int totalLikeCount;
    private int totalCommentCount;
    private int totalDurationSeconds;
    private String formattedTotalDuration;

    @Builder.Default
    private List<VideoPartDetailDto> parts = new ArrayList<>();
}

