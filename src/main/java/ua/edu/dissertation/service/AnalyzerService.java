package ua.edu.dissertation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.ChannelRatingDto;
import ua.edu.dissertation.dto.DefenseDiscussionRatingDto;
import ua.edu.dissertation.dto.DescriptiveStatisticsDto;
import ua.edu.dissertation.dto.MetricType;
import ua.edu.dissertation.dto.ParsedDissertationMetadata;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.SpecialtyRatingDto;
import ua.edu.dissertation.dto.SystemRatingsDto;
import ua.edu.dissertation.dto.VideoMetricStatisticsDto;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.DissertationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyzerService {

    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final String UNKNOWN_CHANNEL_LABEL = "Невідомий канал";
    private static final String UNKNOWN_SPECIALTY_LABEL = "Не вказано";

    private final DefenseVideoRepository defenseVideoRepository;
    private final DissertationRepository dissertationRepository;
    private final ChannelRepository channelRepository;
    private final DissertationMetadataParser metadataParser;
    private final CouncilPresentationAnalyzer councilPresentationAnalyzer;

    @Autowired
    public AnalyzerService(DefenseVideoRepository defenseVideoRepository,
                           DissertationRepository dissertationRepository,
                           ChannelRepository channelRepository,
                           DissertationMetadataParser metadataParser,
                           CouncilPresentationAnalyzer councilPresentationAnalyzer) {
        this.defenseVideoRepository = defenseVideoRepository;
        this.dissertationRepository = dissertationRepository;
        this.channelRepository = channelRepository;
        this.metadataParser = metadataParser;
        this.councilPresentationAnalyzer = councilPresentationAnalyzer;
    }

    public double обчислитиСереднє(List<? extends Number> values) {
        return calculateAverage(values);
    }

    public double calculateAverage(List<? extends Number> values) {
        boolean isSourceEmpty = values == null || values.isEmpty();
        if (isSourceEmpty) {
            return 0.0;
        }

        double sum = 0.0;
        long validCount = 0;
        for (Number item : values) {
            boolean isItemPresent = item != null;
            if (isItemPresent) {
                sum += item.doubleValue();
                validCount++;
            }
        }

        boolean hasNoValidItems = validCount == 0;
        if (hasNoValidItems) {
            return 0.0;
        }
        return sum / validCount;
    }

    public double обчислитиМедіану(List<? extends Number> values) {
        return calculateMedian(values);
    }

    public double calculateMedian(List<? extends Number> values) {
        boolean isSourceEmpty = values == null || values.isEmpty();
        if (isSourceEmpty) {
            return 0.0;
        }

        List<Double> sortedNumbers = values.stream()
                .filter(Objects::nonNull)
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());

        boolean hasNoValidItems = sortedNumbers.isEmpty();
        if (hasNoValidItems) {
            return 0.0;
        }

        int count = sortedNumbers.size();
        boolean isEvenCount = count % 2 == 0;
        if (isEvenCount) {
            int rightMiddleIndex = count / 2;
            int leftMiddleIndex = rightMiddleIndex - 1;
            double leftValue = sortedNumbers.get(leftMiddleIndex);
            double rightValue = sortedNumbers.get(rightMiddleIndex);
            return (leftValue + rightValue) / 2.0;
        } else {
            int middleIndex = count / 2;
            return sortedNumbers.get(middleIndex);
        }
    }

    public DescriptiveStatisticsDto calculateDescriptiveStatistics(List<? extends Number> values) {
        boolean isSourceEmpty = values == null || values.isEmpty();
        if (isSourceEmpty) {
            return DescriptiveStatisticsDto.builder().build();
        }

        List<Double> validNumbers = values.stream()
                .filter(Objects::nonNull)
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());

        boolean hasNoValidItems = validNumbers.isEmpty();
        if (hasNoValidItems) {
            return DescriptiveStatisticsDto.builder().build();
        }

        double sum = 0.0;
        double min = validNumbers.get(0);
        double max = validNumbers.get(validNumbers.size() - 1);
        for (Double value : validNumbers) {
            sum += value;
        }

        double average = sum / validNumbers.size();
        double median = calculateMedian(validNumbers);

        return DescriptiveStatisticsDto.builder()
                .average(average)
                .median(median)
                .count(validNumbers.size())
                .sum(sum)
                .min(min)
                .max(max)
                .build();
    }

    public VideoMetricStatisticsDto обчислитиСереднє() {
        return calculateOverallStatistics();
    }

    public VideoMetricStatisticsDto calculateOverallAverages() {
        return calculateOverallStatistics();
    }

    public VideoMetricStatisticsDto обчислитиМедіану() {
        return calculateOverallStatistics();
    }

    public VideoMetricStatisticsDto calculateOverallMedians() {
        return calculateOverallStatistics();
    }

    public VideoMetricStatisticsDto обчислитиЗагальнуСтатистику() {
        List<DefenseVideo> allVideos = defenseVideoRepository.findAll();
        return calculateOverallStatistics(allVideos);
    }

    public VideoMetricStatisticsDto calculateOverallStatistics() {
        return обчислитиЗагальнуСтатистику();
    }

    public VideoMetricStatisticsDto обчислитиЗагальнуСтатистику(List<DefenseVideo> videos) {
        return calculateOverallStatistics(videos);
    }

    public VideoMetricStatisticsDto calculateOverallStatistics(List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return VideoMetricStatisticsDto.builder()
                    .views(DescriptiveStatisticsDto.builder().build())
                    .likes(DescriptiveStatisticsDto.builder().build())
                    .comments(DescriptiveStatisticsDto.builder().build())
                    .sampleSize(0)
                    .build();
        }

        List<Integer> views = extractMetricList(videos, MetricType.VIEWS);
        List<Integer> likes = extractMetricList(videos, MetricType.LIKES);
        List<Integer> comments = extractMetricList(videos, MetricType.COMMENTS);

        return VideoMetricStatisticsDto.builder()
                .views(calculateDescriptiveStatistics(views))
                .likes(calculateDescriptiveStatistics(likes))
                .comments(calculateDescriptiveStatistics(comments))
                .sampleSize(videos.size())
                .build();
    }

    public double обчислитиСереднє(MetricType metric) {
        return calculateAverage(metric);
    }

    public double calculateAverage(MetricType metric) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return calculateAverage(metric, videos);
    }

    public double обчислитиСереднє(MetricType metric, List<DefenseVideo> videos) {
        return calculateAverage(metric, videos);
    }

    public double calculateAverage(MetricType metric, List<DefenseVideo> videos) {
        List<Integer> values = extractMetricList(videos, metric);
        return calculateAverage(values);
    }

    public double обчислитиМедіану(MetricType metric) {
        return calculateMedian(metric);
    }

    public double calculateMedian(MetricType metric) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return calculateMedian(metric, videos);
    }

    public double обчислитиМедіану(MetricType metric, List<DefenseVideo> videos) {
        return calculateMedian(metric, videos);
    }

    public double calculateMedian(MetricType metric, List<DefenseVideo> videos) {
        List<Integer> values = extractMetricList(videos, metric);
        return calculateMedian(values);
    }

    public Map<String, VideoMetricStatisticsDto> обчислитиСтатистикуЗаКаналами() {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return обчислитиСтатистикуЗаКаналами(videos);
    }

    public Map<String, VideoMetricStatisticsDto> calculateStatisticsByChannel() {
        return обчислитиСтатистикуЗаКаналами();
    }

    public Map<String, VideoMetricStatisticsDto> обчислитиСтатистикуЗаКаналами(List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return Collections.emptyMap();
        }

        Map<String, List<DefenseVideo>> grouped = videos.stream()
                .collect(Collectors.groupingBy(this::resolveChannelName, LinkedHashMap::new, Collectors.toList()));

        Map<String, VideoMetricStatisticsDto> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<DefenseVideo>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), calculateOverallStatistics(entry.getValue()));
        }
        return result;
    }

    public Map<String, Double> обчислитиСереднєЗаКаналами(MetricType metric) {
        return calculateChannelAverages(metric);
    }

    public Map<String, Double> calculateChannelAverages(MetricType metric) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return обчислитиСереднєЗаКаналами(metric, videos);
    }

    public Map<String, Double> обчислитиСереднєЗаКаналами(MetricType metric, List<DefenseVideo> videos) {
        Map<String, VideoMetricStatisticsDto> stats = обчислитиСтатистикуЗаКаналами(videos);
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, VideoMetricStatisticsDto> entry : stats.entrySet()) {
            result.put(entry.getKey(), extractAverageByMetric(entry.getValue(), metric));
        }
        return result;
    }

    public Map<String, Double> обчислитиМедіануЗаКаналами(MetricType metric) {
        return calculateChannelMedians(metric);
    }

    public Map<String, Double> calculateChannelMedians(MetricType metric) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return обчислитиМедіануЗаКаналами(metric, videos);
    }

    public Map<String, Double> обчислитиМедіануЗаКаналами(MetricType metric, List<DefenseVideo> videos) {
        Map<String, VideoMetricStatisticsDto> stats = обчислитиСтатистикуЗаКаналами(videos);
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, VideoMetricStatisticsDto> entry : stats.entrySet()) {
            result.put(entry.getKey(), extractMedianByMetric(entry.getValue(), metric));
        }
        return result;
    }

    public Map<String, VideoMetricStatisticsDto> обчислитиСтатистикуЗаСпеціальностями() {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return обчислитиСтатистикуЗаСпеціальностями(videos);
    }

    public Map<String, VideoMetricStatisticsDto> calculateStatisticsBySpecialty() {
        return обчислитиСтатистикуЗаСпеціальностями();
    }

    public Map<String, VideoMetricStatisticsDto> обчислитиСтатистикуЗаСпеціальностями(List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return Collections.emptyMap();
        }

        Map<String, List<DefenseVideo>> grouped = videos.stream()
                .collect(Collectors.groupingBy(this::resolveSpecialty, LinkedHashMap::new, Collectors.toList()));

        Map<String, VideoMetricStatisticsDto> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<DefenseVideo>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), calculateOverallStatistics(entry.getValue()));
        }
        return result;
    }

    public Map<String, Double> обчислитиСереднєЗаСпеціальностями(MetricType metric) {
        return calculateSpecialtyAverages(metric);
    }

    public Map<String, Double> calculateSpecialtyAverages(MetricType metric) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return обчислитиСереднєЗаСпеціальностями(metric, videos);
    }

    public Map<String, Double> обчислитиСереднєЗаСпеціальностями(MetricType metric, List<DefenseVideo> videos) {
        Map<String, VideoMetricStatisticsDto> stats = обчислитиСтатистикуЗаСпеціальностями(videos);
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, VideoMetricStatisticsDto> entry : stats.entrySet()) {
            result.put(entry.getKey(), extractAverageByMetric(entry.getValue(), metric));
        }
        return result;
    }

    public Map<String, Double> обчислитиМедіануЗаСпеціальностями(MetricType metric) {
        return calculateSpecialtyMedians(metric);
    }

    public Map<String, Double> calculateSpecialtyMedians(MetricType metric) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return обчислитиМедіануЗаСпеціальностями(metric, videos);
    }

    public Map<String, Double> обчислитиМедіануЗаСпеціальностями(MetricType metric, List<DefenseVideo> videos) {
        Map<String, VideoMetricStatisticsDto> stats = обчислитиСтатистикуЗаСпеціальностями(videos);
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, VideoMetricStatisticsDto> entry : stats.entrySet()) {
            result.put(entry.getKey(), extractMedianByMetric(entry.getValue(), metric));
        }
        return result;
    }

    public SystemRatingsDto сформуватиРейтинг() {
        return сформуватиРейтинг(DEFAULT_TOP_LIMIT);
    }

    public SystemRatingsDto generateRatings() {
        return сформуватиРейтинг();
    }

    public SystemRatingsDto generateRatings(int topN) {
        return сформуватиРейтинг(topN);
    }

    public SystemRatingsDto сформуватиРейтинг(int topN) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return сформуватиРейтинг(topN, videos);
    }

    public SystemRatingsDto сформуватиРейтинг(int topN, List<DefenseVideo> videos) {
        int safeLimit = sanitizeLimit(topN);
        List<ChannelRatingDto> topChannels = сформуватиРейтингКаналів(safeLimit, videos);
        List<SpecialtyRatingDto> topSpecialties = сформуватиРейтингСпеціальностей(safeLimit, videos);
        List<DefenseDiscussionRatingDto> topDiscussed = сформуватиРейтингОбговорюванихЗахистів(safeLimit, videos);

        return SystemRatingsDto.builder()
                .topChannels(topChannels)
                .topSpecialties(topSpecialties)
                .topDiscussedDefenses(topDiscussed)
                .build();
    }

    public List<ChannelRatingDto> сформуватиРейтингКаналів(int topN) {
        return getTopChannelsByViews(topN);
    }

    public List<ChannelRatingDto> getTopChannelsByViews(int topN) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return сформуватиРейтингКаналів(topN, videos);
    }

    public List<ChannelRatingDto> сформуватиРейтингКаналів(int topN, List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return Collections.emptyList();
        }

        int safeLimit = sanitizeLimit(topN);
        Map<String, List<DefenseVideo>> grouped = videos.stream()
                .collect(Collectors.groupingBy(this::resolveChannelName, LinkedHashMap::new, Collectors.toList()));

        List<ChannelRatingDto> ratings = new ArrayList<>();
        for (Map.Entry<String, List<DefenseVideo>> entry : grouped.entrySet()) {
            String channelName = entry.getKey();
            List<DefenseVideo> channelVideos = entry.getValue();

            long totalViews = channelVideos.stream()
                    .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                    .sum();
            int videoCount = channelVideos.size();
            double averageViews = videoCount > 0 ? (double) totalViews / videoCount : 0.0;

            String channelId = channelVideos.stream()
                    .map(DefenseVideo::getChannel)
                    .filter(Objects::nonNull)
                    .map(ua.edu.dissertation.domain.entity.Channel::getId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            ratings.add(ChannelRatingDto.builder()
                    .channelId(channelId)
                    .channelName(channelName)
                    .totalViews(totalViews)
                    .videoCount(videoCount)
                    .averageViews(averageViews)
                    .build());
        }

        ratings.sort(Comparator.comparingLong(ChannelRatingDto::getTotalViews).reversed()
                .thenComparing(ChannelRatingDto::getChannelName));

        List<ChannelRatingDto> limited = ratings.stream().limit(safeLimit).collect(Collectors.toList());
        for (int index = 0; index < limited.size(); index++) {
            limited.get(index).setRank(index + 1);
        }
        return limited;
    }

    public List<SpecialtyRatingDto> сформуватиРейтингСпеціальностей(int topN) {
        return getTopSpecialtiesByPopularity(topN);
    }

    public List<SpecialtyRatingDto> getTopSpecialtiesByPopularity(int topN) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return сформуватиРейтингСпеціальностей(topN, videos);
    }

    public List<SpecialtyRatingDto> сформуватиРейтингСпеціальностей(int topN, List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return Collections.emptyList();
        }

        int safeLimit = sanitizeLimit(topN);
        Map<String, List<DefenseVideo>> grouped = videos.stream()
                .collect(Collectors.groupingBy(this::resolveSpecialty, LinkedHashMap::new, Collectors.toList()));

        List<SpecialtyRatingDto> ratings = new ArrayList<>();
        for (Map.Entry<String, List<DefenseVideo>> entry : grouped.entrySet()) {
            String specialty = entry.getKey();
            List<DefenseVideo> specialtyVideos = entry.getValue();

            long totalViews = specialtyVideos.stream()
                    .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                    .sum();

            long distinctDissertationsCount = specialtyVideos.stream()
                    .map(DefenseVideo::getDissertation)
                    .filter(Objects::nonNull)
                    .map(Dissertation::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            boolean hasMappedDissertations = distinctDissertationsCount > 0;
            int defenseCount = hasMappedDissertations ? (int) distinctDissertationsCount : specialtyVideos.size();
            double averageViews = defenseCount > 0 ? (double) totalViews / defenseCount : 0.0;

            ratings.add(SpecialtyRatingDto.builder()
                    .specialty(specialty)
                    .totalViews(totalViews)
                    .defenseCount(defenseCount)
                    .averageViews(averageViews)
                    .build());
        }

        ratings.sort(Comparator.comparingLong(SpecialtyRatingDto::getTotalViews).reversed()
                .thenComparing(SpecialtyRatingDto::getSpecialty));

        List<SpecialtyRatingDto> limited = ratings.stream().limit(safeLimit).collect(Collectors.toList());
        for (int index = 0; index < limited.size(); index++) {
            limited.get(index).setRank(index + 1);
        }
        return limited;
    }

    public List<DefenseDiscussionRatingDto> сформуватиРейтингОбговорюванихЗахистів(int topN) {
        return getTopDiscussedDefenses(topN);
    }

    public List<DefenseDiscussionRatingDto> getTopDiscussedDefenses(int topN) {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return сформуватиРейтингОбговорюванихЗахистів(topN, videos);
    }

    public List<DefenseDiscussionRatingDto> сформуватиРейтингОбговорюванихЗахистів(int topN, List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return Collections.emptyList();
        }

        int safeLimit = sanitizeLimit(topN);

        Map<Long, List<DefenseVideo>> byDissertation = new LinkedHashMap<>();
        List<DefenseVideo> standaloneVideos = new ArrayList<>();

        for (DefenseVideo video : videos) {
            boolean hasDissertation = video.getDissertation() != null && video.getDissertation().getId() != null;
            if (hasDissertation) {
                Long dissertationId = video.getDissertation().getId();
                byDissertation.computeIfAbsent(dissertationId, key -> new ArrayList<>()).add(video);
            } else {
                standaloneVideos.add(video);
            }
        }

        List<DefenseDiscussionRatingDto> ratings = new ArrayList<>();

        for (Map.Entry<Long, List<DefenseVideo>> entry : byDissertation.entrySet()) {
            List<DefenseVideo> defenseVideos = entry.getValue();
            DefenseVideo sampleVideo = defenseVideos.get(0);
            Dissertation dissertation = sampleVideo.getDissertation();

            int totalComments = defenseVideos.stream()
                    .mapToInt(v -> v.getCommentCount() != null ? v.getCommentCount() : 0)
                    .sum();
            int totalViews = defenseVideos.stream()
                    .mapToInt(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                    .sum();
            int totalLikes = defenseVideos.stream()
                    .mapToInt(v -> v.getLikeCount() != null ? v.getLikeCount() : 0)
                    .sum();

            String author = dissertation.getAuthorFullName();
            String topic = dissertation.getTopic();
            String specialty = dissertation.getSpecialty();
            String channelName = resolveChannelName(sampleVideo);

            ratings.add(DefenseDiscussionRatingDto.builder()
                    .dissertationId(dissertation.getId())
                    .authorFullName(author)
                    .topic(topic)
                    .specialty(specialty)
                    .channelName(channelName)
                    .totalComments(totalComments)
                    .totalViews(totalViews)
                    .totalLikes(totalLikes)
                    .partCount(defenseVideos.size())
                    .build());
        }

        for (DefenseVideo video : standaloneVideos) {
            ParsedDissertationMetadata meta = metadataParser.parse(video.getTitle(), video.getDescription());
            int comments = video.getCommentCount() != null ? video.getCommentCount() : 0;
            int views = video.getViewCount() != null ? video.getViewCount() : 0;
            int likes = video.getLikeCount() != null ? video.getLikeCount() : 0;

            String author = meta.getAuthorFullName().isBlank() ? "Здобувач не вказаний" : meta.getAuthorFullName();
            String topic = meta.getTopic().isBlank() ? video.getTitle() : meta.getTopic();
            String specialty = meta.getSpecialty().isBlank() ? resolveSpecialty(video) : meta.getSpecialty();
            String channelName = resolveChannelName(video);

            ratings.add(DefenseDiscussionRatingDto.builder()
                    .dissertationId(video.getId())
                    .authorFullName(author)
                    .topic(topic)
                    .specialty(specialty)
                    .channelName(channelName)
                    .totalComments(comments)
                    .totalViews(views)
                    .totalLikes(likes)
                    .partCount(1)
                    .build());
        }

        ratings.sort(Comparator.comparingInt(DefenseDiscussionRatingDto::getTotalComments).reversed()
                .thenComparing(Comparator.comparingInt(DefenseDiscussionRatingDto::getTotalViews).reversed())
                .thenComparing(DefenseDiscussionRatingDto::getTopic, Comparator.nullsLast(String::compareTo)));

        List<DefenseDiscussionRatingDto> limited = ratings.stream().limit(safeLimit).collect(Collectors.toList());
        for (int index = 0; index < limited.size(); index++) {
            limited.get(index).setRank(index + 1);
        }
        return limited;
    }

    public PresentationComparisonReport порівнятиРади() {
        return compareCouncils();
    }

    public PresentationComparisonReport compareCouncils() {
        return councilPresentationAnalyzer.generateTopUniversitiesComparison();
    }

    private List<Integer> extractMetricList(List<DefenseVideo> videos, MetricType metric) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return Collections.emptyList();
        }

        return videos.stream()
                .map(video -> {
                    if (metric == MetricType.VIEWS) {
                        return video.getViewCount() != null ? video.getViewCount() : 0;
                    }
                    if (metric == MetricType.LIKES) {
                        return video.getLikeCount() != null ? video.getLikeCount() : 0;
                    }
                    return video.getCommentCount() != null ? video.getCommentCount() : 0;
                })
                .collect(Collectors.toList());
    }

    private double extractAverageByMetric(VideoMetricStatisticsDto stats, MetricType metric) {
        if (stats == null) {
            return 0.0;
        }
        return switch (metric) {
            case VIEWS -> stats.getViews() != null ? stats.getViews().getAverage() : 0.0;
            case LIKES -> stats.getLikes() != null ? stats.getLikes().getAverage() : 0.0;
            case COMMENTS -> stats.getComments() != null ? stats.getComments().getAverage() : 0.0;
        };
    }

    private double extractMedianByMetric(VideoMetricStatisticsDto stats, MetricType metric) {
        if (stats == null) {
            return 0.0;
        }
        return switch (metric) {
            case VIEWS -> stats.getViews() != null ? stats.getViews().getMedian() : 0.0;
            case LIKES -> stats.getLikes() != null ? stats.getLikes().getMedian() : 0.0;
            case COMMENTS -> stats.getComments() != null ? stats.getComments().getMedian() : 0.0;
        };
    }

    private String resolveChannelName(DefenseVideo video) {
        boolean hasChannel = video != null && video.getChannel() != null;
        if (hasChannel) {
            String name = video.getChannel().getName();
            boolean hasChannelName = name != null && !name.isBlank();
            if (hasChannelName) {
                return name.trim();
            }
        }
        return UNKNOWN_CHANNEL_LABEL;
    }

    private String resolveSpecialty(DefenseVideo video) {
        if (video == null) {
            return UNKNOWN_SPECIALTY_LABEL;
        }

        boolean hasDissertation = video.getDissertation() != null;
        if (hasDissertation) {
            String dissertationSpecialty = video.getDissertation().getSpecialty();
            boolean hasSpecialty = dissertationSpecialty != null && !dissertationSpecialty.isBlank();
            if (hasSpecialty) {
                return dissertationSpecialty.trim();
            }
        }

        ParsedDissertationMetadata meta = metadataParser.parse(video.getTitle(), video.getDescription());
        String parsedSpecialty = meta.getSpecialty();
        boolean hasParsedSpecialty = parsedSpecialty != null && !parsedSpecialty.isBlank();
        if (hasParsedSpecialty) {
            return parsedSpecialty.trim();
        }

        return UNKNOWN_SPECIALTY_LABEL;
    }

    private int sanitizeLimit(int topN) {
        boolean isNonPositive = topN <= 0;
        if (isNonPositive) {
            return DEFAULT_TOP_LIMIT;
        }
        return topN;
    }
}
