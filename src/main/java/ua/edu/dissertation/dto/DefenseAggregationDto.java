package ua.edu.dissertation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    public LocalDate getFirstPublishedAt() {
        return parts.stream()
                .map(VideoPartDetailDto::getPublishedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    public String getChannelName() {
        return parts.stream()
                .map(VideoPartDetailDto::getChannelName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public boolean isMultiPart() {
        return totalPartsCount > 1;
    }

    public double calculateViewShare(VideoPartDetailDto part) {
        if (part == null || totalViewCount == 0) {
            return 0.0;
        }
        return part.getViewCount() * 100.0 / totalViewCount;
    }
}
