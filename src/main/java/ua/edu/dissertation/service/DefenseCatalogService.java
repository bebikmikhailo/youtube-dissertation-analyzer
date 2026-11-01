package ua.edu.dissertation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.CollectionResultDto;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.VideoFilterCriteria.SortField;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.CouncilRepository;
import ua.edu.dissertation.repository.DissertationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DefenseCatalogService {

    private final VideoQueryService videoQueryService;
    private final VideoPartsProcessor videoPartsProcessor;
    private final DataCollectorService dataCollectorService;
    private final DissertationRepository dissertationRepository;
    private final ChannelRepository channelRepository;
    private final CouncilRepository councilRepository;

    public DefenseCatalogService(VideoQueryService videoQueryService,
                                 VideoPartsProcessor videoPartsProcessor,
                                 DataCollectorService dataCollectorService,
                                 DissertationRepository dissertationRepository,
                                 ChannelRepository channelRepository,
                                 CouncilRepository councilRepository) {
        this.videoQueryService = videoQueryService;
        this.videoPartsProcessor = videoPartsProcessor;
        this.dataCollectorService = dataCollectorService;
        this.dissertationRepository = dissertationRepository;
        this.channelRepository = channelRepository;
        this.councilRepository = councilRepository;
    }

    public List<DefenseAggregationDto> findDefenses(VideoFilterCriteria criteria) {
        VideoFilterCriteria effectiveCriteria = criteria != null ? criteria : new VideoFilterCriteria();
        List<DefenseVideo> matchedVideos = videoQueryService.findVideos(effectiveCriteria);

        List<DefenseAggregationDto> defenses = new ArrayList<>();
        Set<Long> includedDissertationIds = new HashSet<>();
        for (DefenseVideo video : matchedVideos) {
            Dissertation dissertation = video.getDissertation();
            if (dissertation == null) {
                defenses.add(createUnmergedDefense(video));
                continue;
            }
            boolean isFirstPartOfDefense = includedDissertationIds.add(dissertation.getId());
            if (isFirstPartOfDefense) {
                defenses.add(videoPartsProcessor.createAggregatedDto(dissertation));
            }
        }

        defenses.sort(defenseOrder(effectiveCriteria.getSortField(), effectiveCriteria.getSortDirection()));
        return defenses;
    }

    public Optional<DefenseAggregationDto> findDefense(Long dissertationId) {
        if (dissertationId == null) {
            return Optional.empty();
        }
        return dissertationRepository.findById(dissertationId)
                .map(videoPartsProcessor::createAggregatedDto);
    }

    @Transactional(rollbackFor = Exception.class)
    public CollectionResultDto collectDefenses(String query) {
        List<DefenseVideo> collectedVideos = dataCollectorService.searchVideos(query);
        List<Dissertation> mergedDefenses = videoPartsProcessor.processAndMergeVideoParts(collectedVideos);
        log.info("За запитом '{}' збережено {} відео та сформовано {} захистів",
                query, collectedVideos.size(), mergedDefenses.size());
        return new CollectionResultDto(collectedVideos.size(), mergedDefenses.size());
    }

    public List<String> findChannelNames() {
        return channelRepository.findAllNames();
    }

    public List<String> findUniversities() {
        return councilRepository.findDistinctUniversities();
    }

    public List<String> findSpecialties() {
        return dissertationRepository.findDistinctSpecialties();
    }

    private DefenseAggregationDto createUnmergedDefense(DefenseVideo video) {
        Dissertation transientDefense = Dissertation.builder()
                .topic(video.getTitle())
                .videos(List.of(video))
                .build();
        return videoPartsProcessor.createAggregatedDto(transientDefense);
    }

    private Comparator<DefenseAggregationDto> defenseOrder(SortField field, Sort.Direction direction) {
        SortField effectiveField = field != null ? field : SortField.PUBLISHED_AT;
        Comparator<DefenseAggregationDto> ascendingOrder = switch (effectiveField) {
            case VIEWS -> Comparator.comparingInt(DefenseAggregationDto::getTotalViewCount);
            case LIKES -> Comparator.comparingInt(DefenseAggregationDto::getTotalLikeCount);
            case COMMENTS -> Comparator.comparingInt(DefenseAggregationDto::getTotalCommentCount);
            case DURATION -> Comparator.comparingInt(DefenseAggregationDto::getTotalDurationSeconds);
            case PUBLISHED_AT -> Comparator.comparing(DefenseAggregationDto::getFirstPublishedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
        };
        boolean isAscending = direction == Sort.Direction.ASC;
        return isAscending ? ascendingOrder : ascendingOrder.reversed();
    }
}

