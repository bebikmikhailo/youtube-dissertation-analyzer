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
public class DefenseDiscussionRatingDto {

    private int rank;
    private Long dissertationId;
    private String authorFullName;
    private String topic;
    private String specialty;
    private String channelName;
    private int totalComments;
    private int totalViews;
    private int totalLikes;
    private int partCount;
}

