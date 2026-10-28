package ua.edu.dissertation.client.youtube.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeVideoDto {

    private String id;
    private String title;
    private String description;
    private LocalDate publishedAt;
    private int durationSeconds;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private boolean hasSubtitles;
    private String channelId;
    private String channelTitle;
}
