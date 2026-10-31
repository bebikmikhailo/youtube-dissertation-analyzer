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
public class ChannelRatingDto {

    private int rank;
    private String channelId;
    private String channelName;
    private long totalViews;
    private int videoCount;
    private double averageViews;
}

