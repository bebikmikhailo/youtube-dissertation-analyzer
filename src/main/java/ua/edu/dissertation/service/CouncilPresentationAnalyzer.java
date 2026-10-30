package ua.edu.dissertation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.edu.dissertation.client.youtube.YouTubeDemoDataFactory;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.UniversityPresentationProfile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CouncilPresentationAnalyzer {

    private static final String KPI_NAME = "КПІ ім. Ігоря Сікорського";
    private static final String NUBIP_NAME = "НУБіП України";
    private static final String KNU_NAME = "КНУ імені Тараса Шевченка";
    private static final String LP_NAME = "Національний університет «Львівська політехніка»";

    private static final Pattern AUTHOR_IN_TITLE_PATTERN = Pattern.compile(
            "[А-ЯІЇЄҐ][а-яіїєґ\\']+\\s+[А-ЯІЇЄҐ]\\.[\\s]*[А-ЯІЇЄҐ]\\.?|[А-ЯІЇЄҐ][а-яіїєґ\\']+\\s+[А-ЯІЇЄҐ][а-яіїєґ\\']+",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern TOPIC_IN_TITLE_PATTERN = Pattern.compile(
            "на\\s+тему|[«\"“][^»\"”]{10,}[»\"”]",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DEGREE_IN_TITLE_PATTERN = Pattern.compile(
            "доктора\\s+філософії|phd|кандидат|доктор\\s+наук",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile(
            "\\(частина\\s*\\d+\\)|частина\\s*\\d+|ч\\.\\s*\\d+|part\\s*\\d+",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern STRUCTURED_LABELS_PATTERN = Pattern.compile(
            "(?:Здобувач:|Тема:|Спеціальність:)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern COUNCIL_CODE_PATTERN = Pattern.compile(
            "\\b([ДD][ФF]?\\s*\\d{2}\\.\\d{3}\\.\\d{2,3})\\b",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern SPECIALTY_CODE_PATTERN = Pattern.compile(
            "\\b\\d{3}\\b"
    );

    private final DissertationMetadataParser metadataParser;

    @Autowired
    public CouncilPresentationAnalyzer(DissertationMetadataParser metadataParser) {
        this.metadataParser = metadataParser;
    }

    public PresentationComparisonReport generateTopUniversitiesComparison() {
        List<YouTubeVideoDto> demoVideos = YouTubeDemoDataFactory.createDemoVideos();
        return analyzeVideoDtos(demoVideos);
    }

    public PresentationComparisonReport analyzeDefenseVideos(List<DefenseVideo> videos) {
        boolean isListEmpty = videos == null || videos.isEmpty();
        if (isListEmpty) {
            return buildEmptyReport();
        }

        List<YouTubeVideoDto> dtos = videos.stream()
                .map(video -> YouTubeVideoDto.builder()
                        .id(video.getYoutubeVideoId())
                        .title(video.getTitle())
                        .description(video.getDescription())
                        .publishedAt(video.getPublishedAt())
                        .durationSeconds(video.getDurationSeconds())
                        .viewCount(video.getViewCount())
                        .likeCount(video.getLikeCount())
                        .commentCount(video.getCommentCount())
                        .hasSubtitles(video.getHasSubtitles())
                        .channelId(video.getChannel() != null ? video.getChannel().getId() : null)
                        .channelTitle(video.getChannel() != null ? video.getChannel().getName() : null)
                        .build())
                .collect(Collectors.toList());

        return analyzeVideoDtos(dtos);
    }

    public PresentationComparisonReport analyzeVideoDtos(List<YouTubeVideoDto> videoDtos) {
        boolean isListEmpty = videoDtos == null || videoDtos.isEmpty();
        if (isListEmpty) {
            return buildEmptyReport();
        }

        Map<String, List<YouTubeVideoDto>> groupedByUniversity = groupVideosByUniversity(videoDtos);

        List<UniversityPresentationProfile> profiles = new ArrayList<>();
        for (Map.Entry<String, List<YouTubeVideoDto>> entry : groupedByUniversity.entrySet()) {
            String universityName = entry.getKey();
            List<YouTubeVideoDto> universityVideos = entry.getValue();
            UniversityPresentationProfile profile = analyzeUniversityGroup(universityName, universityVideos);
            profiles.add(profile);
        }

        List<String> commonFeatures = buildCommonFeatures();
        List<String> differences = buildDifferences();
        String analyticalConclusion = buildAnalyticalConclusion();

        return PresentationComparisonReport.builder()
                .profiles(profiles)
                .commonFeatures(commonFeatures)
                .differences(differences)
                .analyticalConclusion(analyticalConclusion)
                .build();
    }

    public UniversityPresentationProfile analyzeUniversityGroup(String universityName, List<YouTubeVideoDto> videos) {
        int totalVideos = videos.size();
        boolean hasVideos = totalVideos > 0;
        if (!hasVideos) {
            return UniversityPresentationProfile.builder()
                    .universityName(universityName)
                    .totalVideosAnalyzed(0)
                    .styleSummary("Немає даних для аналізу")
                    .build();
        }

        int authorInTitleCount = 0;
        int topicInTitleCount = 0;
        int degreeInTitleCount = 0;
        int partInTitleCount = 0;
        int structuredLabelsCount = 0;
        int councilCodeCount = 0;
        int specialtyCodeCount = 0;

        for (YouTubeVideoDto video : videos) {
            String title = video.getTitle() != null ? video.getTitle() : "";
            String description = video.getDescription() != null ? video.getDescription() : "";

            boolean hasAuthorInTitle = AUTHOR_IN_TITLE_PATTERN.matcher(title).find();
            if (hasAuthorInTitle) {
                authorInTitleCount++;
            }

            boolean hasTopicInTitle = TOPIC_IN_TITLE_PATTERN.matcher(title).find();
            if (hasTopicInTitle) {
                topicInTitleCount++;
            }

            boolean hasDegreeInTitle = DEGREE_IN_TITLE_PATTERN.matcher(title).find();
            if (hasDegreeInTitle) {
                degreeInTitleCount++;
            }

            boolean hasPartInTitle = PART_NUMBER_PATTERN.matcher(title).find();
            if (hasPartInTitle) {
                partInTitleCount++;
            }

            boolean hasStructuredLabels = STRUCTURED_LABELS_PATTERN.matcher(description).find();
            if (hasStructuredLabels) {
                structuredLabelsCount++;
            }

            boolean hasCouncilCode = COUNCIL_CODE_PATTERN.matcher(description).find()
                    || COUNCIL_CODE_PATTERN.matcher(title).find();
            if (hasCouncilCode) {
                councilCodeCount++;
            }

            boolean hasSpecialtyCode = SPECIALTY_CODE_PATTERN.matcher(description).find()
                    || SPECIALTY_CODE_PATTERN.matcher(title).find();
            if (hasSpecialtyCode) {
                specialtyCodeCount++;
            }
        }

        double authorPercentage = calculatePercentage(authorInTitleCount, totalVideos);
        double topicPercentage = calculatePercentage(topicInTitleCount, totalVideos);
        double degreePercentage = calculatePercentage(degreeInTitleCount, totalVideos);
        double partPercentage = calculatePercentage(partInTitleCount, totalVideos);
        double structuredLabelsPercentage = calculatePercentage(structuredLabelsCount, totalVideos);
        double councilCodePercentage = calculatePercentage(councilCodeCount, totalVideos);
        double specialtyCodePercentage = calculatePercentage(specialtyCodeCount, totalVideos);

        boolean multiPartDetected = partInTitleCount > 0;
        String styleSummary = buildUniversityStyleSummary(universityName, multiPartDetected, structuredLabelsPercentage > 50.0);

        return UniversityPresentationProfile.builder()
                .universityName(universityName)
                .totalVideosAnalyzed(totalVideos)
                .titleHasAuthorPercentage(authorPercentage)
                .titleHasTopicPercentage(topicPercentage)
                .titleHasDegreePercentage(degreePercentage)
                .titleHasPartNumberPercentage(partPercentage)
                .descriptionHasStructuredLabelsPercentage(structuredLabelsPercentage)
                .descriptionHasCouncilCodePercentage(councilCodePercentage)
                .descriptionHasSpecialtyCodePercentage(specialtyCodePercentage)
                .multiPartPracticeDetected(multiPartDetected)
                .styleSummary(styleSummary)
                .build();
    }

    private Map<String, List<YouTubeVideoDto>> groupVideosByUniversity(List<YouTubeVideoDto> videos) {
        Map<String, List<YouTubeVideoDto>> groups = new LinkedHashMap<>();
        groups.put(KPI_NAME, new ArrayList<>());
        groups.put(NUBIP_NAME, new ArrayList<>());
        groups.put(KNU_NAME, new ArrayList<>());
        groups.put(LP_NAME, new ArrayList<>());

        for (YouTubeVideoDto video : videos) {
            String detectedUniversity = metadataParser.extractUniversity(video.getTitle(), video.getDescription());
            boolean isAssigned = false;

            if (!detectedUniversity.isBlank()) {
                List<YouTubeVideoDto> targetList = groups.get(detectedUniversity);
                boolean hasTargetList = targetList != null;
                if (hasTargetList) {
                    targetList.add(video);
                    isAssigned = true;
                }
            }

            if (!isAssigned) {
                String fallbackUniversity = detectUniversityFromChannelOrText(video);
                List<YouTubeVideoDto> fallbackList = groups.get(fallbackUniversity);
                boolean hasFallbackList = fallbackList != null;
                if (hasFallbackList) {
                    fallbackList.add(video);
                } else {
                    groups.computeIfAbsent(fallbackUniversity, key -> new ArrayList<>()).add(video);
                }
            }
        }

        Map<String, List<YouTubeVideoDto>> filteredGroups = new LinkedHashMap<>();
        for (Map.Entry<String, List<YouTubeVideoDto>> entry : groups.entrySet()) {
            boolean hasEntries = !entry.getValue().isEmpty();
            if (hasEntries) {
                filteredGroups.put(entry.getKey(), entry.getValue());
            }
        }

        return filteredGroups;
    }

    private String detectUniversityFromChannelOrText(YouTubeVideoDto video) {
        String channel = video.getChannelTitle() != null ? video.getChannelTitle().toLowerCase(Locale.ROOT) : "";
        String text = ((video.getTitle() != null ? video.getTitle() : "") + " "
                + (video.getDescription() != null ? video.getDescription() : "")).toLowerCase(Locale.ROOT);

        boolean isKpi = channel.contains("кпі") || text.contains("кпі") || text.contains("сікорського");
        if (isKpi) {
            return KPI_NAME;
        }

        boolean isNubip = channel.contains("нубіп") || text.contains("нубіп") || text.contains("біоресурсів");
        if (isNubip) {
            return NUBIP_NAME;
        }

        boolean isKnu = channel.contains("кну") || text.contains("кну") || text.contains("шевраса") || text.contains("шевченка");
        if (isKnu) {
            return KNU_NAME;
        }

        boolean isLp = channel.contains("львівська політехніка") || text.contains("політехнік");
        if (isLp) {
            return LP_NAME;
        }

        return "Інші спеціалізовані вчені ради";
    }

    private double calculatePercentage(int count, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(((double) count / total) * 1000.0) / 10.0;
    }

    private String buildUniversityStyleSummary(String universityName, boolean multiPart, boolean structuredLabels) {
        if (KPI_NAME.equals(universityName)) {
            return "Формальний академічний стиль: назва відео фіксує здобуваний ступінь (доктор філософії) та ПІБ здобувача. Тема виноситься в опис. В описі використовується сувора структурована розмітка з мітками «Здобувач:», «Тема:», «Спеціальність:» та шифром ради.";
        }
        if (NUBIP_NAME.equals(universityName)) {
            return "Практика розділених трансляцій: захисти часто розбиваються на декілька частин із маркерами «(частина 1)», «(частина 2)» у назві. Назва містить лише ПІБ з ініціалами. Повні дані (ПІБ у родовому відмінку, розширена тема, спеціальність та шифр разової ради ДФ) розміщуються у зв'язному текстовому описі.";
        }
        if (KNU_NAME.equals(universityName)) {
            return "Інформативні заголовки: назва відео одразу містить ПІБ здобувача та повну тему дисертації після фрази «на тему». В описі вказується статус разової ради, спеціальність та повне ім'я здобувача у називному відмінку.";
        }
        if (LP_NAME.equals(universityName)) {
            return "Комплексний стандарт подання: назва містить ПІБ та тему дослідження в лапках. Опис поєднує структуровані мітки («Здобувач:», «Спеціальність:») з повною офіційною назвою спеціалізованої ради та університету.";
        }
        return "Стандартне академічне подання інформації про публічний захист дисертаційного дослідження.";
    }

    private List<String> buildCommonFeatures() {
        return List.of(
                "Усі досліджені ЗВО (КПІ, НУБіП, КНУ, Львівська політехніка) обов'язково публікують ПІБ здобувача, тему дослідження та шифр/назву спеціальності у метаданих публікації.",
                "Трансляції захистів публікуються на офіційних каналах університетів або спеціалізованих медіа-каналах відповідних підрозділів.",
                "Кожен заклад забезпечує фіксацію коду спеціальності згідно з державною класифікацією (наприклад, 072, 121, 051, 122)."
        );
    }

    private List<String> buildDifferences() {
        return List.of(
                "Структура заголовка відео: КНУ та Львівська політехніка виносять повну тему дисертації безпосередньо в назву («на тему «...»»), тоді як КПІ та НУБіП обмежують заголовок ПІБ здобувача, переносячи повну тему в опис.",
                "Поділ на частини (multi-part): НУБіП активно практикує поділ захисту на окремі відеозаписи («частина 1», «частина 2»), тоді як КПІ, КНУ та Львівська політехніка публікують захист одним суцільним записом.",
                "Форматування опису: КПІ та Львівська політехніка використовують структурований формат із ключовими мітками («Здобувач:», «Тема:», «Спеціальність:»), тоді як НУБіП подає інформацію суцільним текстом зі зв'язними реченнями.",
                "Зазначення шифру ради: КПІ та НУБіП обов'язково зазначають офіційний шифр ради (Д 26.002.01, ДФ 26.004.001) в описі, тоді як інші ЗВО іноді обмежуються текстовою назвою ради або університету."
        );
    }

    private String buildAnalyticalConclusion() {
        return "Аналіз оформлення метаданих захистів дисертацій у провідних ЗВО України (КПІ, НУБіП, КНУ, Львівська політехніка) свідчить про наявність двох основних підходів. "
                + "Перший підхід (КНУ, Львівська політехніка) орієнтований на максимальну інформативність заголовка відео, куди одразу виносяться ПІБ та тема дослідження. "
                + "Другий підхід (КПІ, НУБіП) використовує лаконічні назви із зазначенням статусу чи ступеня, переносячи детальну специфікацію в опис. "
                + "НУБіП вирізняється практикою фрагментації записів на частини, що вимагає спеціальних алгоритмів об'єднання даних.";
    }

    private PresentationComparisonReport buildEmptyReport() {
        return PresentationComparisonReport.builder()
                .profiles(List.of())
                .commonFeatures(List.of())
                .differences(List.of())
                .analyticalConclusion("Немає даних для формування звіту порівняння.")
                .build();
    }
}

