package ua.edu.dissertation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;
import ua.edu.dissertation.dto.ChannelRatingDto;
import ua.edu.dissertation.dto.CorrelationAnalysisDto;
import ua.edu.dissertation.dto.CorrelationResultDto;
import ua.edu.dissertation.dto.DefenseDiscussionRatingDto;
import ua.edu.dissertation.dto.DescriptiveStatisticsDto;
import ua.edu.dissertation.dto.DynamicsInterval;
import ua.edu.dissertation.dto.MetricType;
import ua.edu.dissertation.dto.ParsedDissertationMetadata;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.SpecialtyRatingDto;
import ua.edu.dissertation.dto.SystemRatingsDto;
import ua.edu.dissertation.dto.ViewDynamicsPointDto;
import ua.edu.dissertation.dto.ViewDynamicsReportDto;
import ua.edu.dissertation.dto.VideoMetricStatisticsDto;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.DissertationRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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
    private final StatisticsSnapshotRepository statisticsSnapshotRepository;
    private final DissertationMetadataParser metadataParser;
    private final CouncilPresentationAnalyzer councilPresentationAnalyzer;

    @Autowired
    public AnalyzerService(DefenseVideoRepository defenseVideoRepository,
                           DissertationRepository dissertationRepository,
                           ChannelRepository channelRepository,
                           StatisticsSnapshotRepository statisticsSnapshotRepository,
                           DissertationMetadataParser metadataParser,
                           CouncilPresentationAnalyzer councilPresentationAnalyzer) {
        this.defenseVideoRepository = defenseVideoRepository;
        this.dissertationRepository = dissertationRepository;
        this.channelRepository = channelRepository;
        this.statisticsSnapshotRepository = statisticsSnapshotRepository;
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

    public double обчислитиКоефіцієнтПірсона(List<? extends Number> xList, List<? extends Number> yList) {
        return calculatePearsonCorrelation(xList, yList);
    }

    public double calculatePearsonCorrelation(List<? extends Number> xList, List<? extends Number> yList) {
        boolean isXEmpty = xList == null || xList.isEmpty();
        boolean isYEmpty = yList == null || yList.isEmpty();
        boolean hasEmptyInput = isXEmpty || isYEmpty;
        if (hasEmptyInput) {
            return 0.0;
        }

        List<Double> validX = new ArrayList<>();
        List<Double> validY = new ArrayList<>();
        int minSize = Math.min(xList.size(), yList.size());

        for (int i = 0; i < minSize; i++) {
            Number xItem = xList.get(i);
            Number yItem = yList.get(i);
            boolean hasBothValues = xItem != null && yItem != null;
            if (hasBothValues) {
                validX.add(xItem.doubleValue());
                validY.add(yItem.doubleValue());
            }
        }

        int pairCount = validX.size();
        boolean hasInsufficientPairs = pairCount < 2;
        if (hasInsufficientPairs) {
            return 0.0;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < pairCount; i++) {
            sumX += validX.get(i);
            sumY += validY.get(i);
        }
        double meanX = sumX / pairCount;
        double meanY = sumY / pairCount;

        double sumProductDiff = 0.0;
        double sumDiffSqX = 0.0;
        double sumDiffSqY = 0.0;

        for (int i = 0; i < pairCount; i++) {
            double diffX = validX.get(i) - meanX;
            double diffY = validY.get(i) - meanY;
            sumProductDiff += diffX * diffY;
            sumDiffSqX += diffX * diffX;
            sumDiffSqY += diffY * diffY;
        }

        double varianceProduct = sumDiffSqX * sumDiffSqY;
        boolean isZeroVarianceX = sumDiffSqX <= 1e-15;
        boolean isZeroVarianceY = sumDiffSqY <= 1e-15;
        boolean hasZeroVariance = isZeroVarianceX || isZeroVarianceY || varianceProduct <= 1e-15;
        if (hasZeroVariance) {
            return 0.0;
        }

        double denominator = Math.sqrt(varianceProduct);
        boolean isInvalidDenominator = Double.isNaN(denominator) || Double.isInfinite(denominator) || denominator == 0.0;
        if (isInvalidDenominator) {
            return 0.0;
        }

        double correlation = sumProductDiff / denominator;
        boolean isAboveMax = correlation > 1.0;
        if (isAboveMax) {
            return 1.0;
        }
        boolean isBelowMin = correlation < -1.0;
        if (isBelowMin) {
            return -1.0;
        }

        return correlation;
    }

    public String інтерпретуватиКореляцію(double coefficient) {
        return interpretCorrelation(coefficient);
    }

    public String interpretCorrelation(double coefficient) {
        boolean isUndefined = Double.isNaN(coefficient) || Double.isInfinite(coefficient);
        if (isUndefined) {
            return "Кореляційний зв'язок відсутній";
        }

        double absoluteValue = Math.abs(coefficient);
        boolean isZero = absoluteValue < 1e-4;
        if (isZero) {
            return "Кореляційний зв'язок відсутній";
        }

        boolean isPositive = coefficient > 0.0;
        String direction = isPositive ? "позитивний" : "негативний";

        boolean isVeryStrong = absoluteValue >= 0.9;
        if (isVeryStrong) {
            return "Дуже сильний " + direction + " кореляційний зв'язок";
        }

        boolean isStrong = absoluteValue >= 0.7;
        if (isStrong) {
            return "Сильний " + direction + " кореляційний зв'язок";
        }

        boolean isModerate = absoluteValue >= 0.5;
        if (isModerate) {
            return "Помірний " + direction + " кореляційний зв'язок";
        }

        boolean isWeak = absoluteValue >= 0.3;
        if (isWeak) {
            return "Слабкий " + direction + " кореляційний зв'язок";
        }

        return "Дуже слабкий " + direction + " кореляційний зв'язок";
    }

    public String отриматиСилуКореляції(double coefficient) {
        return getCorrelationStrength(coefficient);
    }

    public String getCorrelationStrength(double coefficient) {
        boolean isUndefined = Double.isNaN(coefficient) || Double.isInfinite(coefficient);
        if (isUndefined) {
            return "Відсутня";
        }

        double absoluteValue = Math.abs(coefficient);
        boolean isZero = absoluteValue < 1e-4;
        if (isZero) {
            return "Відсутня";
        }

        boolean isVeryStrong = absoluteValue >= 0.9;
        if (isVeryStrong) {
            return "Дуже сильна";
        }

        boolean isStrong = absoluteValue >= 0.7;
        if (isStrong) {
            return "Сильна";
        }

        boolean isModerate = absoluteValue >= 0.5;
        if (isModerate) {
            return "Помірна";
        }

        boolean isWeak = absoluteValue >= 0.3;
        if (isWeak) {
            return "Слабка";
        }

        return "Дуже слабка";
    }

    public CorrelationAnalysisDto обчислитиКореляції() {
        return calculateCorrelations();
    }

    public CorrelationAnalysisDto calculateCorrelations() {
        List<DefenseVideo> videos = defenseVideoRepository.findAll();
        return calculateCorrelations(videos, LocalDate.now());
    }

    public CorrelationAnalysisDto обчислитиКореляції(List<DefenseVideo> videos) {
        return calculateCorrelations(videos);
    }

    public CorrelationAnalysisDto calculateCorrelations(List<DefenseVideo> videos) {
        return calculateCorrelations(videos, LocalDate.now());
    }

    public CorrelationAnalysisDto обчислитиКореляції(List<DefenseVideo> videos, LocalDate referenceDate) {
        return calculateCorrelations(videos, referenceDate);
    }

    public CorrelationAnalysisDto calculateCorrelations(List<DefenseVideo> videos, LocalDate referenceDate) {
        LocalDate effectiveDate = referenceDate != null ? referenceDate : LocalDate.now();
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return CorrelationAnalysisDto.builder()
                    .durationCorrelation(buildCorrelationResult("Тривалість відео", Collections.emptyList(), Collections.emptyList()))
                    .ageDaysCorrelation(buildCorrelationResult("Дні з моменту публікації", Collections.emptyList(), Collections.emptyList()))
                    .subtitlesCorrelation(buildCorrelationResult("Наявність субтитрів", Collections.emptyList(), Collections.emptyList()))
                    .allCorrelations(Collections.emptyList())
                    .totalVideosAnalyzed(0)
                    .build();
        }

        List<Integer> durationValues = new ArrayList<>();
        List<Integer> durationViews = new ArrayList<>();

        List<Long> ageDaysValues = new ArrayList<>();
        List<Integer> ageDaysViews = new ArrayList<>();

        List<Double> subtitleValues = new ArrayList<>();
        List<Integer> subtitleViews = new ArrayList<>();

        for (DefenseVideo video : videos) {
            boolean isVideoPresent = video != null;
            if (isVideoPresent) {
                int views = video.getViewCount() != null ? video.getViewCount() : 0;
                int duration = video.getDurationSeconds() != null ? video.getDurationSeconds() : 0;
                durationValues.add(duration);
                durationViews.add(views);

                boolean hasPublishedAt = video.getPublishedAt() != null;
                if (hasPublishedAt) {
                    long days = ChronoUnit.DAYS.between(video.getPublishedAt(), effectiveDate);
                    boolean isFutureDate = days < 0;
                    long normalizedDays = isFutureDate ? 0 : days;
                    ageDaysValues.add(normalizedDays);
                    ageDaysViews.add(views);
                }

                double hasSubtitlesValue = Boolean.TRUE.equals(video.getHasSubtitles()) ? 1.0 : 0.0;
                subtitleValues.add(hasSubtitlesValue);
                subtitleViews.add(views);
            }
        }

        CorrelationResultDto durationCorrelation = buildCorrelationResult("Тривалість відео", durationValues, durationViews);
        CorrelationResultDto ageDaysCorrelation = buildCorrelationResult("Дні з моменту публікації", ageDaysValues, ageDaysViews);
        CorrelationResultDto subtitlesCorrelation = buildCorrelationResult("Наявність субтитрів", subtitleValues, subtitleViews);

        List<CorrelationResultDto> allCorrelations = List.of(durationCorrelation, ageDaysCorrelation, subtitlesCorrelation);

        return CorrelationAnalysisDto.builder()
                .durationCorrelation(durationCorrelation)
                .ageDaysCorrelation(ageDaysCorrelation)
                .subtitlesCorrelation(subtitlesCorrelation)
                .allCorrelations(allCorrelations)
                .totalVideosAnalyzed(videos.size())
                .build();
    }

    private CorrelationResultDto buildCorrelationResult(String factorName,
                                                        List<? extends Number> xValues,
                                                        List<? extends Number> yValues) {
        double coefficient = calculatePearsonCorrelation(xValues, yValues);
        String interpretation = interpretCorrelation(coefficient);
        String strength = getCorrelationStrength(coefficient);
        int sampleSize = Math.min(xValues != null ? xValues.size() : 0, yValues != null ? yValues.size() : 0);
        boolean isPositive = coefficient > 0.0;

        return CorrelationResultDto.builder()
                .factorName(factorName)
                .coefficient(coefficient)
                .strength(strength)
                .interpretation(interpretation)
                .sampleSize(sampleSize)
                .isPositive(isPositive)
                .build();
    }

    public ViewDynamicsReportDto побудуватиДинаміку() {
        return buildViewDynamics();
    }

    public ViewDynamicsReportDto buildViewDynamics() {
        return buildViewDynamics(DynamicsInterval.DAYS);
    }

    public ViewDynamicsReportDto побудуватиДинаміку(DynamicsInterval interval) {
        return buildViewDynamics(interval);
    }

    public ViewDynamicsReportDto buildViewDynamics(DynamicsInterval interval) {
        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository.findAllByOrderByCollectedAtAsc();
        return buildViewDynamics(snapshots, interval);
    }

    public ViewDynamicsReportDto побудуватиДинаміку(Long videoId, DynamicsInterval interval) {
        return buildViewDynamics(videoId, interval);
    }

    public ViewDynamicsReportDto buildViewDynamics(Long videoId, DynamicsInterval interval) {
        boolean isIdNull = videoId == null;
        if (isIdNull) {
            return buildEmptyDynamicsReport(interval);
        }
        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository.findByVideoIdOrderByCollectedAtAsc(videoId);
        return buildViewDynamics(snapshots, interval);
    }

    public ViewDynamicsReportDto побудуватиДинаміку(String youtubeVideoId, DynamicsInterval interval) {
        return buildViewDynamics(youtubeVideoId, interval);
    }

    public ViewDynamicsReportDto buildViewDynamics(String youtubeVideoId, DynamicsInterval interval) {
        boolean isVideoIdBlank = youtubeVideoId == null || youtubeVideoId.isBlank();
        if (isVideoIdBlank) {
            return buildEmptyDynamicsReport(interval);
        }
        List<StatisticsSnapshot> snapshots = statisticsSnapshotRepository.findByVideoYoutubeVideoIdOrderByCollectedAtAsc(youtubeVideoId);
        return buildViewDynamics(snapshots, interval);
    }

    public ViewDynamicsReportDto побудуватиДинаміку(List<StatisticsSnapshot> snapshots, DynamicsInterval interval) {
        return buildViewDynamics(snapshots, interval);
    }

    public ViewDynamicsReportDto buildViewDynamics(List<StatisticsSnapshot> snapshots, DynamicsInterval interval) {
        DynamicsInterval effectiveInterval = interval != null ? interval : DynamicsInterval.DAYS;
        boolean isSourceEmpty = snapshots == null || snapshots.isEmpty();
        if (isSourceEmpty) {
            return buildEmptyDynamicsReport(effectiveInterval);
        }

        List<StatisticsSnapshot> sortedSnapshots = snapshots.stream()
                .filter(s -> s != null && s.getCollectedAt() != null)
                .sorted(Comparator.comparing(StatisticsSnapshot::getCollectedAt))
                .collect(Collectors.toList());

        boolean hasNoValidSnapshots = sortedSnapshots.isEmpty();
        if (hasNoValidSnapshots) {
            return buildEmptyDynamicsReport(effectiveInterval);
        }

        Map<LocalDate, List<StatisticsSnapshot>> bucketedSnapshots = new TreeMap<>();
        for (StatisticsSnapshot snapshot : sortedSnapshots) {
            LocalDate bucketDate = resolveBucketDate(snapshot.getCollectedAt().toLocalDate(), effectiveInterval);
            bucketedSnapshots.computeIfAbsent(bucketDate, k -> new ArrayList<>()).add(snapshot);
        }

        List<ViewDynamicsPointDto> points = new ArrayList<>();
        long previousViews = -1;

        for (Map.Entry<LocalDate, List<StatisticsSnapshot>> entry : bucketedSnapshots.entrySet()) {
            LocalDate bucketDate = entry.getKey();
            List<StatisticsSnapshot> currentSnapshots = entry.getValue();

            Map<String, StatisticsSnapshot> latestSnapshotByVideo = new LinkedHashMap<>();
            for (StatisticsSnapshot snapshot : currentSnapshots) {
                String videoKey = resolveSnapshotVideoKey(snapshot);
                StatisticsSnapshot existing = latestSnapshotByVideo.get(videoKey);
                boolean isNewerSnapshot = existing == null
                        || snapshot.getCollectedAt().isAfter(existing.getCollectedAt());
                if (isNewerSnapshot) {
                    latestSnapshotByVideo.put(videoKey, snapshot);
                }
            }

            long bucketViews = 0;
            long bucketLikes = 0;
            long bucketComments = 0;

            for (StatisticsSnapshot latest : latestSnapshotByVideo.values()) {
                bucketViews += latest.getViews() != null ? latest.getViews() : 0;
                bucketLikes += latest.getLikes() != null ? latest.getLikes() : 0;
                bucketComments += latest.getComments() != null ? latest.getComments() : 0;
            }

            int videoCount = latestSnapshotByVideo.size();
            double averageViews = videoCount > 0 ? (double) bucketViews / videoCount : 0.0;

            long viewsGrowth = 0;
            boolean hasBaseline = previousViews >= 0;
            if (hasBaseline) {
                long delta = bucketViews - previousViews;
                boolean isPositiveDelta = delta > 0;
                viewsGrowth = isPositiveDelta ? delta : 0;
            }
            previousViews = bucketViews;

            String periodLabel = resolvePeriodLabel(bucketDate, effectiveInterval);
            points.add(ViewDynamicsPointDto.builder()
                    .period(periodLabel)
                    .date(bucketDate)
                    .totalViews(bucketViews)
                    .viewsGrowth(viewsGrowth)
                    .totalLikes(bucketLikes)
                    .totalComments(bucketComments)
                    .snapshotCount(currentSnapshots.size())
                    .videoCount(videoCount)
                    .averageViews(averageViews)
                    .build());
        }

        boolean hasPoints = !points.isEmpty();
        long initialViews = hasPoints ? points.get(0).getTotalViews() : 0;
        long latestViews = hasPoints ? points.get(points.size() - 1).getTotalViews() : 0;
        long totalGrowth = Math.max(0, latestViews - initialViews);

        return ViewDynamicsReportDto.builder()
                .interval(effectiveInterval)
                .points(points)
                .initialViews(initialViews)
                .latestViews(latestViews)
                .totalGrowth(totalGrowth)
                .totalSnapshots(sortedSnapshots.size())
                .build();
    }

    private LocalDate resolveBucketDate(LocalDate date, DynamicsInterval interval) {
        if (date == null) {
            return LocalDate.now();
        }
        if (interval == DynamicsInterval.WEEKS) {
            return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (interval == DynamicsInterval.MONTHS) {
            return date.withDayOfMonth(1);
        }
        return date;
    }

    private String resolvePeriodLabel(LocalDate date, DynamicsInterval interval) {
        if (date == null) {
            return "";
        }
        if (interval == DynamicsInterval.MONTHS) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String resolveSnapshotVideoKey(StatisticsSnapshot snapshot) {
        boolean hasVideo = snapshot.getVideo() != null;
        if (hasVideo) {
            boolean hasVideoId = snapshot.getVideo().getId() != null;
            if (hasVideoId) {
                return "id:" + snapshot.getVideo().getId();
            }
            boolean hasYoutubeId = snapshot.getVideo().getYoutubeVideoId() != null;
            if (hasYoutubeId) {
                return "yt:" + snapshot.getVideo().getYoutubeVideoId();
            }
        }
        return "snapshot:" + snapshot.getId();
    }

    private ViewDynamicsReportDto buildEmptyDynamicsReport(DynamicsInterval interval) {
        return ViewDynamicsReportDto.builder()
                .interval(interval != null ? interval : DynamicsInterval.DAYS)
                .points(Collections.emptyList())
                .initialViews(0)
                .latestViews(0)
                .totalGrowth(0)
                .totalSnapshots(0)
                .build();
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
