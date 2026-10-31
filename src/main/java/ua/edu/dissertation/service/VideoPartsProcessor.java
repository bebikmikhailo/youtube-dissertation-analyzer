package ua.edu.dissertation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.Council;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.ParsedDissertationMetadata;
import ua.edu.dissertation.dto.VideoPartDetailDto;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.CouncilRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.DissertationRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class VideoPartsProcessor {

    private static final Pattern PART_MARKER_PATTERN = Pattern.compile(
            "(?:\\(частина|частина|част\\.|ч\\.|\\(part|part)\\s*(\\d+)\\)?",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private final DissertationMetadataParser metadataParser;
    private final DissertationRepository dissertationRepository;
    private final DefenseVideoRepository defenseVideoRepository;
    private final CouncilRepository councilRepository;
    private final ChannelRepository channelRepository;

    @Autowired
    public VideoPartsProcessor(DissertationMetadataParser metadataParser,
                               DissertationRepository dissertationRepository,
                               DefenseVideoRepository defenseVideoRepository,
                               CouncilRepository councilRepository,
                               ChannelRepository channelRepository) {
        this.metadataParser = metadataParser;
        this.dissertationRepository = dissertationRepository;
        this.defenseVideoRepository = defenseVideoRepository;
        this.councilRepository = councilRepository;
        this.channelRepository = channelRepository;
    }

    public boolean hasPartMarker(String title) {
        boolean isBlank = title == null || title.isBlank();
        if (isBlank) {
            return false;
        }
        return PART_MARKER_PATTERN.matcher(title).find();
    }

    public int extractPartNumber(String title) {
        boolean isBlank = title == null || title.isBlank();
        if (isBlank) {
            return 1;
        }

        Matcher matcher = PART_MARKER_PATTERN.matcher(title);
        boolean hasMatch = matcher.find();
        if (hasMatch) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException exception) {
                return 1;
            }
        }
        return 1;
    }

    public String extractBaseTitle(String title) {
        boolean isBlank = title == null || title.isBlank();
        if (isBlank) {
            return "";
        }

        String cleaned = title.replaceAll("(?i)\\((?:частина|part)\\s*\\d+\\)", "")
                .replaceAll("(?i)\\b(?:частина|част\\.|ч\\.|part)\\s*\\d+\\b", "")
                .replaceAll("[-–—()\\[\\]]+$", "")
                .trim()
                .replaceAll("\\s+", " ");

        return cleaned;
    }

    public boolean shouldMerge(DefenseVideo video1, DefenseVideo video2) {
        boolean isEitherNull = video1 == null || video2 == null;
        if (isEitherNull) {
            return false;
        }

        boolean isSameObject = Objects.equals(video1.getId(), video2.getId()) && video1.getId() != null;
        if (isSameObject) {
            return true;
        }

        String channel1 = video1.getChannel() != null ? video1.getChannel().getId() : null;
        String channel2 = video2.getChannel() != null ? video2.getChannel().getId() : null;

        return shouldMerge(
                video1.getTitle(),
                video1.getDescription(),
                video1.getPublishedAt(),
                channel1,
                video2.getTitle(),
                video2.getDescription(),
                video2.getPublishedAt(),
                channel2
        );
    }

    public boolean shouldMerge(String title1, String description1, LocalDate date1, String channel1,
                               String title2, String description2, LocalDate date2, String channel2) {
        boolean hasBothChannels = channel1 != null && !channel1.isBlank() && channel2 != null && !channel2.isBlank();
        boolean areChannelsDifferent = hasBothChannels && !Objects.equals(channel1, channel2);
        if (areChannelsDifferent) {
            return false;
        }

        boolean hasBothDates = date1 != null && date2 != null;
        if (hasBothDates) {
            long daysDifference = Math.abs(ChronoUnit.DAYS.between(date1, date2));
            boolean isOutsideDateThreshold = daysDifference > 3;
            if (isOutsideDateThreshold) {
                return false;
            }
        }

        String author1 = metadataParser.extractAuthor(title1, description1);
        String author2 = metadataParser.extractAuthor(title2, description2);
        boolean hasBothAuthors = !author1.isBlank() && !author2.isBlank();
        if (hasBothAuthors) {
            boolean areAuthorsMatched = areAuthorsCompatible(author1, author2);
            if (!areAuthorsMatched) {
                return false;
            }
        }

        boolean hasMarker1 = hasPartMarker(title1);
        boolean hasMarker2 = hasPartMarker(title2);
        boolean hasPartMarkers = hasMarker1 || hasMarker2;

        String baseTitle1 = extractBaseTitle(title1);
        String baseTitle2 = extractBaseTitle(title2);
        double titleSimilarity = calculateTitleSimilarity(baseTitle1, baseTitle2);
        boolean areBaseTitlesSimilar = titleSimilarity >= 0.70;

        if (hasBothAuthors) {
            return areBaseTitlesSimilar || hasPartMarkers;
        }

        return areBaseTitlesSimilar && hasPartMarkers;
    }

    public boolean areAuthorsCompatible(String author1, String author2) {
        boolean isEitherBlank = author1 == null || author2 == null || author1.isBlank() || author2.isBlank();
        if (isEitherBlank) {
            return false;
        }

        String normalizedFirst = metadataParser.normalizeAuthorNameToNominative(author1);
        String normalizedSecond = metadataParser.normalizeAuthorNameToNominative(author2);

        boolean isExactMatch = normalizedFirst.equalsIgnoreCase(normalizedSecond);
        if (isExactMatch) {
            return true;
        }

        String[] tokensFirst = normalizedFirst.split("\\s+");
        String[] tokensSecond = normalizedSecond.split("\\s+");

        String surnameFirst = tokensFirst[0].toLowerCase(Locale.ROOT);
        String surnameSecond = tokensSecond[0].toLowerCase(Locale.ROOT);

        boolean areSurnamesEqual = surnameFirst.equals(surnameSecond);
        if (!areSurnamesEqual) {
            return false;
        }

        boolean hasTokenFirst = tokensFirst.length > 1;
        boolean hasTokenSecond = tokensSecond.length > 1;
        if (hasTokenFirst && hasTokenSecond) {
            char initialFirst = Character.toLowerCase(tokensFirst[1].charAt(0));
            char initialSecond = Character.toLowerCase(tokensSecond[1].charAt(0));
            return initialFirst == initialSecond;
        }

        return true;
    }

    public double calculateTitleSimilarity(String title1, String title2) {
        boolean isEitherNull = title1 == null || title2 == null;
        if (isEitherNull) {
            return 0.0;
        }

        String normalizedFirst = title1.toLowerCase(Locale.ROOT).trim();
        String normalizedSecond = title2.toLowerCase(Locale.ROOT).trim();

        boolean isExactMatch = normalizedFirst.equals(normalizedSecond);
        if (isExactMatch) {
            return 1.0;
        }
        boolean isEitherEmpty = normalizedFirst.isEmpty() || normalizedSecond.isEmpty();
        if (isEitherEmpty) {
            return 0.0;
        }

        Set<String> wordsFirst = Arrays.stream(normalizedFirst.split("[\\s,.:;«»\"'()-]+"))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());

        Set<String> wordsSecond = Arrays.stream(normalizedSecond.split("[\\s,.:;«»\"'()-]+"))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());

        Set<String> intersection = new HashSet<>(wordsFirst);
        intersection.retainAll(wordsSecond);

        Set<String> union = new HashSet<>(wordsFirst);
        union.addAll(wordsSecond);

        boolean isUnionEmpty = union.isEmpty();
        if (isUnionEmpty) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    public List<List<DefenseVideo>> clusterVideosIntoDefenses(List<DefenseVideo> videos) {
        List<List<DefenseVideo>> clusters = new ArrayList<>();
        boolean isInputEmpty = videos == null || videos.isEmpty();
        if (isInputEmpty) {
            return clusters;
        }

        for (DefenseVideo video : videos) {
            boolean addedToCluster = false;
            for (List<DefenseVideo> cluster : clusters) {
                boolean matchesCluster = false;
                for (DefenseVideo member : cluster) {
                    if (shouldMerge(video, member)) {
                        matchesCluster = true;
                        break;
                    }
                }
                if (matchesCluster) {
                    cluster.add(video);
                    addedToCluster = true;
                    break;
                }
            }
            if (!addedToCluster) {
                List<DefenseVideo> newCluster = new ArrayList<>();
                newCluster.add(video);
                clusters.add(newCluster);
            }
        }

        return clusters;
    }

    @Transactional
    public List<Dissertation> processAndMergeVideoParts(List<DefenseVideo> videos) {
        boolean isInputEmpty = videos == null || videos.isEmpty();
        if (isInputEmpty) {
            return List.of();
        }

        List<DefenseVideo> managedVideos = new ArrayList<>();
        for (DefenseVideo video : videos) {
            DefenseVideo managed = video.getId() != null
                    ? defenseVideoRepository.findById(video.getId()).orElse(video)
                    : video;
            int part = extractPartNumber(managed.getTitle());
            managed.setPartNumber(part);
            managedVideos.add(managed);
        }

        List<List<DefenseVideo>> clusters = clusterVideosIntoDefenses(managedVideos);
        List<Dissertation> mergedList = new ArrayList<>();

        for (List<DefenseVideo> cluster : clusters) {
            cluster.sort(Comparator.comparingInt(DefenseVideo::getPartNumber));
            Dissertation dissertation = resolveOrCreateDissertation(cluster);
            mergedList.add(dissertation);
        }

        return mergedList;
    }

    @Transactional(readOnly = true)
    public DefenseAggregationDto createAggregatedDto(Dissertation dissertation) {
        if (dissertation == null) {
            return DefenseAggregationDto.builder().build();
        }

        Long dissertationId = dissertation.getId();
        Dissertation targetDissertation = dissertationId != null
                ? dissertationRepository.findById(dissertationId).orElse(dissertation)
                : dissertation;

        List<DefenseVideo> videos = targetDissertation.getVideos() != null
                ? new ArrayList<>(targetDissertation.getVideos())
                : new ArrayList<>();
        videos.sort(Comparator.comparingInt(DefenseVideo::getPartNumber));

        int totalViews = videos.stream().mapToInt(v -> v.getViewCount() != null ? v.getViewCount() : 0).sum();
        int totalLikes = videos.stream().mapToInt(v -> v.getLikeCount() != null ? v.getLikeCount() : 0).sum();
        int totalComments = videos.stream().mapToInt(v -> v.getCommentCount() != null ? v.getCommentCount() : 0).sum();
        int totalDuration = videos.stream().mapToInt(v -> v.getDurationSeconds() != null ? v.getDurationSeconds() : 0).sum();

        List<VideoPartDetailDto> partDtos = new ArrayList<>();
        for (DefenseVideo video : videos) {
            int duration = video.getDurationSeconds() != null ? video.getDurationSeconds() : 0;
            String channelName = resolveChannelName(video);

            partDtos.add(VideoPartDetailDto.builder()
                    .videoId(video.getId())
                    .youtubeVideoId(video.getYoutubeVideoId())
                    .partNumber(video.getPartNumber() != null ? video.getPartNumber() : 1)
                    .title(video.getTitle())
                    .publishedAt(video.getPublishedAt())
                    .durationSeconds(duration)
                    .formattedDuration(formatDuration(duration))
                    .viewCount(video.getViewCount() != null ? video.getViewCount() : 0)
                    .likeCount(video.getLikeCount() != null ? video.getLikeCount() : 0)
                    .commentCount(video.getCommentCount() != null ? video.getCommentCount() : 0)
                    .channelName(channelName)
                    .build());
        }

        String councilName = targetDissertation.getCouncil() != null ? targetDissertation.getCouncil().getName() : null;
        String universityName = targetDissertation.getCouncil() != null ? targetDissertation.getCouncil().getUniversity() : null;

        return DefenseAggregationDto.builder()
                .dissertationId(targetDissertation.getId())
                .authorFullName(targetDissertation.getAuthorFullName())
                .topic(targetDissertation.getTopic())
                .specialty(targetDissertation.getSpecialty())
                .councilName(councilName)
                .universityName(universityName)
                .totalPartsCount(videos.size())
                .totalViewCount(totalViews)
                .totalLikeCount(totalLikes)
                .totalCommentCount(totalComments)
                .totalDurationSeconds(totalDuration)
                .formattedTotalDuration(formatDuration(totalDuration))
                .parts(partDtos)
                .build();
    }

    private String resolveChannelName(DefenseVideo video) {
        if (video.getChannel() == null) {
            return null;
        }

        try {
            return video.getChannel().getName();
        } catch (Exception exception) {
            String channelId = video.getChannel().getId();
            boolean hasChannelId = channelId != null;
            if (hasChannelId) {
                return channelRepository.findById(channelId)
                        .map(Channel::getName)
                        .orElse(null);
            }
            return null;
        }
    }

    public List<DefenseAggregationDto> createAggregatedDtos(List<Dissertation> dissertations) {
        boolean isInputEmpty = dissertations == null || dissertations.isEmpty();
        if (isInputEmpty) {
            return List.of();
        }

        List<DefenseAggregationDto> dtoList = new ArrayList<>();
        for (Dissertation dissertation : dissertations) {
            dtoList.add(createAggregatedDto(dissertation));
        }
        return dtoList;
    }

    public String formatDuration(int totalSeconds) {
        boolean isZeroOrNegative = totalSeconds <= 0;
        if (isZeroOrNegative) {
            return "0 с";
        }

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        boolean hasHours = hours > 0;
        boolean hasMinutes = minutes > 0;
        boolean hasSeconds = seconds > 0;

        StringBuilder builder = new StringBuilder();
        if (hasHours) {
            builder.append(hours).append(" год");
        }
        if (hasMinutes) {
            if (hasHours) {
                builder.append(" ");
            }
            builder.append(minutes).append(" хв");
        }
        if (hasSeconds || (!hasHours && !hasMinutes)) {
            if (hasHours || hasMinutes) {
                builder.append(" ");
            }
            builder.append(seconds).append(" с");
        }

        return builder.toString();
    }

    private Dissertation resolveOrCreateDissertation(List<DefenseVideo> cluster) {
        Dissertation existingDissertation = cluster.stream()
                .map(DefenseVideo::getDissertation)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        String resolvedAuthor = "";
        String resolvedTopic = "";
        String resolvedSpecialty = "";
        String resolvedCouncilName = "";
        String resolvedUniversity = "";

        for (DefenseVideo video : cluster) {
            ParsedDissertationMetadata meta = metadataParser.parse(video.getTitle(), video.getDescription());

            boolean hasAuthor = !meta.getAuthorFullName().isBlank();
            boolean hasThreeWordAuthor = meta.getAuthorFullName().split("\\s+").length == 3;
            boolean needAuthorUpdate = resolvedAuthor.isBlank()
                    || (hasThreeWordAuthor && resolvedAuthor.split("\\s+").length < 3);
            if (hasAuthor && needAuthorUpdate) {
                resolvedAuthor = meta.getAuthorFullName();
            }

            boolean hasTopic = !meta.getTopic().isBlank();
            if (hasTopic && resolvedTopic.isBlank()) {
                resolvedTopic = meta.getTopic();
            }

            boolean hasSpecialty = !meta.getSpecialty().isBlank();
            if (hasSpecialty && resolvedSpecialty.isBlank()) {
                resolvedSpecialty = meta.getSpecialty();
            }

            boolean hasCouncil = !meta.getCouncil().isBlank();
            if (hasCouncil && resolvedCouncilName.isBlank()) {
                resolvedCouncilName = meta.getCouncil();
            }

            boolean hasUniversity = !meta.getUniversity().isBlank();
            if (hasUniversity && resolvedUniversity.isBlank()) {
                resolvedUniversity = meta.getUniversity();
            }
        }

        if (resolvedAuthor.isBlank()) {
            resolvedAuthor = "Невідомий здобувач";
        }
        if (resolvedTopic.isBlank()) {
            resolvedTopic = extractBaseTitle(cluster.get(0).getTitle());
        }

        Council council = null;
        boolean hasCouncilOrUniversity = !resolvedCouncilName.isBlank() || !resolvedUniversity.isBlank();
        if (hasCouncilOrUniversity) {
            final String finalCouncilKey = !resolvedCouncilName.isBlank() ? resolvedCouncilName : resolvedUniversity;
            final String finalUniversity = resolvedUniversity;
            council = councilRepository.findByName(finalCouncilKey)
                    .orElseGet(() -> councilRepository.save(Council.builder()
                            .name(finalCouncilKey)
                            .university(finalUniversity)
                            .build()));
        }

        Dissertation dissertation = existingDissertation;
        if (dissertation == null) {
            dissertation = Dissertation.builder()
                    .authorFullName(resolvedAuthor)
                    .topic(resolvedTopic)
                    .specialty(resolvedSpecialty)
                    .council(council)
                    .build();
        } else {
            boolean lacksAuthor = dissertation.getAuthorFullName() == null || dissertation.getAuthorFullName().isBlank();
            if (lacksAuthor) {
                dissertation.setAuthorFullName(resolvedAuthor);
            }
            boolean lacksTopic = dissertation.getTopic() == null || dissertation.getTopic().isBlank();
            if (lacksTopic) {
                dissertation.setTopic(resolvedTopic);
            }
            boolean lacksSpecialty = dissertation.getSpecialty() == null || dissertation.getSpecialty().isBlank();
            if (lacksSpecialty) {
                dissertation.setSpecialty(resolvedSpecialty);
            }
            boolean lacksCouncil = dissertation.getCouncil() == null && council != null;
            if (lacksCouncil) {
                dissertation.setCouncil(council);
            }
        }

        Dissertation savedDissertation = dissertationRepository.save(dissertation);

        for (DefenseVideo video : cluster) {
            DefenseVideo managedVideo = video.getId() != null
                    ? defenseVideoRepository.findById(video.getId()).orElse(video)
                    : video;
            managedVideo.setDissertation(savedDissertation);
            boolean isVideoAlreadyInList = savedDissertation.getVideos().contains(managedVideo);
            if (!isVideoAlreadyInList) {
                savedDissertation.getVideos().add(managedVideo);
            }
            defenseVideoRepository.save(managedVideo);
        }

        return savedDissertation;
    }
}
