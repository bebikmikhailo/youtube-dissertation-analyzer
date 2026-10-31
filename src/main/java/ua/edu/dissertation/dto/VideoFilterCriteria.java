package ua.edu.dissertation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoFilterCriteria {

    private String channelName;
    private String councilOrUniversity;
    private String specialty;
    private LocalDate publishedFrom;
    private LocalDate publishedTo;
    private Integer minViews;
    private Integer maxViews;
    private String searchText;

    @Builder.Default
    private SortField sortField = SortField.PUBLISHED_AT;

    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.DESC;

    @Getter
    public enum SortField {
        VIEWS("viewCount"),
        LIKES("likeCount"),
        COMMENTS("commentCount"),
        PUBLISHED_AT("publishedAt"),
        DURATION("durationSeconds");

        private final String propertyName;

        SortField(String propertyName) {
            this.propertyName = propertyName;
        }
    }
}

