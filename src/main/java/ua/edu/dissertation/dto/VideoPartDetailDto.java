package ua.edu.dissertation.dto;

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
public class VideoPartDetailDto {

    private Long videoId;
    private String youtubeVideoId;
    private int partNumber;
    private String title;
    private LocalDate publishedAt;
    private int durationSeconds;
    private String formattedDuration;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private String channelName;
}

