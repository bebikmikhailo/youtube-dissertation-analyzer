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
public class ViewDynamicsPointDto {

    private String period;
    private LocalDate date;
    private long totalViews;
    private long viewsGrowth;
    private long totalLikes;
    private long totalComments;
    private int snapshotCount;
    private int videoCount;
    private double averageViews;
}

