package ua.edu.dissertation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.dissertation.client.youtube.YouTubeDataApiClient;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DataCollectorService {

    private final DefenseVideoRepository defenseVideoRepository;
    private final StatisticsSnapshotRepository statisticsSnapshotRepository;
    private final ChannelRepository channelRepository;
    private final YouTubeDataApiClient youTubeDataApiClient;

    @Autowired
    public DataCollectorService(DefenseVideoRepository defenseVideoRepository,
                                StatisticsSnapshotRepository statisticsSnapshotRepository,
                                ChannelRepository channelRepository,
                                YouTubeDataApiClient youTubeDataApiClient) {
        this.defenseVideoRepository = defenseVideoRepository;
        this.statisticsSnapshotRepository = statisticsSnapshotRepository;
        this.channelRepository = channelRepository;
        this.youTubeDataApiClient = youTubeDataApiClient;
    }

    public List<DefenseVideo> searchVideos(String query) {
        return searchVideos(query, true);
    }

    public List<DefenseVideo> searchVideos(String query, boolean fallbackToDemoOnQuota) {
        boolean isQueryBlank = query == null || query.isBlank();
        if (isQueryBlank) {
            return List.of();
        }

        List<YouTubeVideoDto> videoDtos = youTubeDataApiClient.searchVideos(query, fallbackToDemoOnQuota);
        return saveMetadata(videoDtos);
    }

    public DefenseVideo saveMetadata(YouTubeVideoDto dto) {
        boolean isDtoInvalid = dto == null || dto.getId() == null || dto.getId().isBlank();
        if (isDtoInvalid) {
            throw new IllegalArgumentException("Ідентифікатор відео YouTube не може бути порожнім");
        }

        Channel channel = resolveChannel(dto);

        DefenseVideo video = defenseVideoRepository.findByYoutubeVideoId(dto.getId())
                .orElseGet(() -> DefenseVideo.builder()
                        .youtubeVideoId(dto.getId())
                        .partNumber(1)
                        .build());

        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());

        boolean hasPublishedAt = dto.getPublishedAt() != null;
        if (hasPublishedAt) {
            video.setPublishedAt(dto.getPublishedAt());
        }

        video.setDurationSeconds(dto.getDurationSeconds());
        video.setViewCount(dto.getViewCount());
        video.setLikeCount(dto.getLikeCount());
        video.setCommentCount(dto.getCommentCount());
        video.setHasSubtitles(dto.isHasSubtitles());

        boolean hasChannel = channel != null;
        if (hasChannel) {
            video.setChannel(channel);
        }

        DefenseVideo savedVideo = defenseVideoRepository.save(video);
        createStatisticsSnapshot(savedVideo);
        return savedVideo;
    }

    public List<DefenseVideo> saveMetadata(List<YouTubeVideoDto> dtos) {
        boolean isListEmpty = dtos == null || dtos.isEmpty();
        if (isListEmpty) {
            return List.of();
        }

        List<DefenseVideo> savedVideos = new ArrayList<>();
        for (YouTubeVideoDto dto : dtos) {
            DefenseVideo savedVideo = saveMetadata(dto);
            savedVideos.add(savedVideo);
        }
        return savedVideos;
    }

    public List<DefenseVideo> updateStatistics() {
        List<DefenseVideo> allVideos = defenseVideoRepository.findAll();
        boolean hasNoVideos = allVideos.isEmpty();
        if (hasNoVideos) {
            return List.of();
        }

        List<String> videoIds = allVideos.stream()
                .map(DefenseVideo::getYoutubeVideoId)
                .collect(Collectors.toList());

        return updateStatistics(videoIds);
    }

    public List<DefenseVideo> updateStatistics(List<String> youtubeVideoIds) {
        boolean isIdsEmpty = youtubeVideoIds == null || youtubeVideoIds.isEmpty();
        if (isIdsEmpty) {
            return List.of();
        }

        List<YouTubeVideoDto> freshDataList = youTubeDataApiClient.getVideosByIds(youtubeVideoIds, true);
        Map<String, YouTubeVideoDto> freshDataById = freshDataList.stream()
                .collect(Collectors.toMap(YouTubeVideoDto::getId, dto -> dto, (existing, replacement) -> existing));

        List<DefenseVideo> updatedVideos = new ArrayList<>();
        for (String videoId : youtubeVideoIds) {
            YouTubeVideoDto freshDto = freshDataById.get(videoId);
            boolean hasFreshData = freshDto != null;
            if (!hasFreshData) {
                continue;
            }

            Optional<DefenseVideo> videoOptional = defenseVideoRepository.findByYoutubeVideoId(videoId);
            boolean isVideoPresent = videoOptional.isPresent();
            if (isVideoPresent) {
                DefenseVideo video = videoOptional.get();
                video.setViewCount(freshDto.getViewCount());
                video.setLikeCount(freshDto.getLikeCount());
                video.setCommentCount(freshDto.getCommentCount());

                boolean hasNewTitle = freshDto.getTitle() != null && !freshDto.getTitle().isBlank();
                if (hasNewTitle) {
                    video.setTitle(freshDto.getTitle());
                }

                DefenseVideo savedVideo = defenseVideoRepository.save(video);
                createStatisticsSnapshot(savedVideo);
                updatedVideos.add(savedVideo);
            }
        }

        return updatedVideos;
    }

    public Optional<DefenseVideo> updateStatisticsForVideo(String youtubeVideoId) {
        boolean isIdBlank = youtubeVideoId == null || youtubeVideoId.isBlank();
        if (isIdBlank) {
            return Optional.empty();
        }

        List<DefenseVideo> updatedVideos = updateStatistics(List.of(youtubeVideoId));
        boolean hasUpdatedVideo = !updatedVideos.isEmpty();
        if (hasUpdatedVideo) {
            return Optional.of(updatedVideos.get(0));
        }

        return Optional.empty();
    }

    public StatisticsSnapshot createStatisticsSnapshot(DefenseVideo video) {
        return createStatisticsSnapshot(video, LocalDateTime.now());
    }

    public StatisticsSnapshot createStatisticsSnapshot(DefenseVideo video, LocalDateTime snapshotTime) {
        boolean isVideoNull = video == null || video.getId() == null;
        if (isVideoNull) {
            throw new IllegalArgumentException("Відео для збереження знімка статистики повинно існувати в базі даних");
        }

        LocalDateTime recordedAt = snapshotTime != null ? snapshotTime : LocalDateTime.now();
        StatisticsSnapshot snapshot = StatisticsSnapshot.builder()
                .collectedAt(recordedAt)
                .views(video.getViewCount())
                .likes(video.getLikeCount())
                .comments(video.getCommentCount())
                .video(video)
                .build();

        video.addSnapshot(snapshot);
        return statisticsSnapshotRepository.save(snapshot);
    }

    private Channel resolveChannel(YouTubeVideoDto dto) {
        boolean hasChannelInfo = dto.getChannelId() != null && !dto.getChannelId().isBlank();
        if (!hasChannelInfo) {
            return null;
        }

        return channelRepository.findById(dto.getChannelId())
                .map(existingChannel -> {
                    boolean hasNewTitle = dto.getChannelTitle() != null && !dto.getChannelTitle().isBlank();
                    boolean isTitleChanged = hasNewTitle && !Objects.equals(existingChannel.getName(), dto.getChannelTitle());
                    if (isTitleChanged) {
                        existingChannel.setName(dto.getChannelTitle());
                    }
                    return existingChannel;
                })
                .orElseGet(() -> {
                    boolean hasCustomTitle = dto.getChannelTitle() != null && !dto.getChannelTitle().isBlank();
                    String channelName = hasCustomTitle ? dto.getChannelTitle() : dto.getChannelId();
                    Channel newChannel = Channel.builder()
                            .id(dto.getChannelId())
                            .name(channelName)
                            .build();
                    return channelRepository.save(newChannel);
                });
    }
}

