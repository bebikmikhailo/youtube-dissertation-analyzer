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
public class SystemRatingsDto {

    @Builder.Default
    private List<ChannelRatingDto> topChannels = new ArrayList<>();

    @Builder.Default
    private List<SpecialtyRatingDto> topSpecialties = new ArrayList<>();

    @Builder.Default
    private List<DefenseDiscussionRatingDto> topDiscussedDefenses = new ArrayList<>();
}

