package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.Council;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.VideoPartDetailDto;
import ua.edu.dissertation.repository.ChannelRepository;
import ua.edu.dissertation.repository.CouncilRepository;
import ua.edu.dissertation.repository.DefenseVideoRepository;
import ua.edu.dissertation.repository.DissertationRepository;
import ua.edu.dissertation.repository.StatisticsSnapshotRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TC_NFR_03_MultiPartMergeAccuracyTest {

    @Autowired
    private VideoPartsProcessor videoPartsProcessor;

    @Autowired
    private DefenseVideoRepository defenseVideoRepository;

    @Autowired
    private DissertationRepository dissertationRepository;

    @Autowired
    private CouncilRepository councilRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private StatisticsSnapshotRepository statisticsSnapshotRepository;

    @BeforeEach
    void setUp() {
        statisticsSnapshotRepository.deleteAll();
        defenseVideoRepository.deleteAll();
        dissertationRepository.deleteAll();
        councilRepository.deleteAll();
        channelRepository.deleteAll();
    }

    private static class MergeTestCase {
        final String title1;
        final String desc1;
        final LocalDate date1;
        final String channel1;
        final String title2;
        final String desc2;
        final LocalDate date2;
        final String channel2;
        final boolean expectedMerge;
        final String description;

        MergeTestCase(String title1, String desc1, LocalDate date1, String channel1,
                      String title2, String desc2, LocalDate date2, String channel2,
                      boolean expectedMerge, String description) {
            this.title1 = title1;
            this.desc1 = desc1;
            this.date1 = date1;
            this.channel1 = channel1;
            this.title2 = title2;
            this.desc2 = desc2;
            this.date2 = date2;
            this.channel2 = channel2;
            this.expectedMerge = expectedMerge;
            this.description = description;
        }
    }

    @Test
    @DisplayName("NFR-03: Точність алгоритму розпізнавання та об'єднання частин захисту >= 90% на 25 контрольних кейсах")
    void shouldAchieveAtLeastNinetyPercentAccuracyOnTwentyFiveControlCases() {
        List<MergeTestCase> controlCases = createControlCases();

        int correctPredictions = 0;
        int totalCases = controlCases.size();

        for (MergeTestCase testCase : controlCases) {
            boolean actualDecision = videoPartsProcessor.shouldMerge(
                    testCase.title1, testCase.desc1, testCase.date1, testCase.channel1,
                    testCase.title2, testCase.desc2, testCase.date2, testCase.channel2
            );

            boolean isPredictionCorrect = actualDecision == testCase.expectedMerge;
            if (isPredictionCorrect) {
                correctPredictions++;
            }
        }

        double accuracy = ((double) correctPredictions / totalCases) * 100.0;
        boolean isAccuracySufficient = accuracy >= 90.0;

        assertThat(isAccuracySufficient).isTrue();
    }

    @Test
    @DisplayName("FR-05 / FR-06 / FR-07: Наскрізне об'єднання частин захисту Лабенка О.М. та агрегація статистики")
    void shouldMergeLabenkoPartsAndCalculateAggregatedStatistics() {
        Channel channel = Channel.builder()
                .id("UC_NUBIP_OFFICIAL")
                .name("НУБіП України")
                .build();
        channel = channelRepository.save(channel);

        Council council = Council.builder()
                .name("Разова спеціалізована вчена рада ДФ 26.004.001")
                .university("НУБіП України")
                .build();
        council = councilRepository.save(council);

        DefenseVideo partOne = DefenseVideo.builder()
                .youtubeVideoId("labenko_p1")
                .title("Захист дисертації Лабенка Олександра Миколайовича (частина 1)")
                .description("Разова спеціалізована вчена рада ДФ 26.004.001. Захист дисертації Лабенка Олександра Миколайовича на тему «Фінансове забезпечення інноваційного розвитку аграрних підприємств» за спеціальністю 072 Фінанси, банківська справа та страхування.")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(8860) // 2 год 27 хв 40 с
                .viewCount(256)
                .likeCount(18)
                .commentCount(4)
                .hasSubtitles(true)
                .channel(channel)
                .build();

        DefenseVideo partTwo = DefenseVideo.builder()
                .youtubeVideoId("labenko_p2")
                .title("Захист дисертації Лабенка Олександра Миколайовича (частина 2)")
                .description("Продовження засідання разової ради ДФ 26.004.001 захисту дисертації Лабенка Олександра Миколайовича.")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(1617) // 26 хв 57 с
                .viewCount(115)
                .likeCount(11)
                .commentCount(2)
                .hasSubtitles(true)
                .channel(channel)
                .build();

        partOne = defenseVideoRepository.save(partOne);
        partTwo = defenseVideoRepository.save(partTwo);

        List<Dissertation> mergedDissertations = videoPartsProcessor.processAndMergeVideoParts(List.of(partOne, partTwo));

        boolean isSingleDissertationCreated = mergedDissertations.size() == 1;
        assertThat(isSingleDissertationCreated).isTrue();

        Dissertation dissertation = mergedDissertations.get(0);
        boolean isAuthorNormalized = "Лабенко Олександр Миколайович".equals(dissertation.getAuthorFullName());
        assertThat(isAuthorNormalized).isTrue();

        DefenseAggregationDto aggregatedDto = videoPartsProcessor.createAggregatedDto(dissertation);

        boolean isPartsCountCorrect = aggregatedDto.getTotalPartsCount() == 2;
        boolean isTotalViewsCorrect = aggregatedDto.getTotalViewCount() == (256 + 115); // 371
        boolean isTotalLikesCorrect = aggregatedDto.getTotalLikeCount() == (18 + 11);   // 29
        boolean isTotalCommentsCorrect = aggregatedDto.getTotalCommentCount() == (4 + 2); // 6
        boolean isTotalDurationCorrect = aggregatedDto.getTotalDurationSeconds() == (8860 + 1617); // 10477

        assertThat(isPartsCountCorrect).isTrue();
        assertThat(isTotalViewsCorrect).isTrue();
        assertThat(isTotalLikesCorrect).isTrue();
        assertThat(isTotalCommentsCorrect).isTrue();
        assertThat(isTotalDurationCorrect).isTrue();

        String formattedDuration = aggregatedDto.getFormattedTotalDuration();
        boolean isFormattedDurationCorrect = formattedDuration.contains("2 год") && formattedDuration.contains("54 хв");
        assertThat(isFormattedDurationCorrect).isTrue();

        List<VideoPartDetailDto> parts = aggregatedDto.getParts();
        boolean hasDetailedParts = parts.size() == 2;
        assertThat(hasDetailedParts).isTrue();

        VideoPartDetailDto firstPart = parts.get(0);
        VideoPartDetailDto secondPart = parts.get(1);

        boolean arePartNumbersCorrect = firstPart.getPartNumber() == 1 && secondPart.getPartNumber() == 2;
        boolean arePartViewsCorrect = firstPart.getViewCount() == 256 && secondPart.getViewCount() == 115;
        assertThat(arePartNumbersCorrect).isTrue();
        assertThat(arePartViewsCorrect).isTrue();
    }

    private List<MergeTestCase> createControlCases() {
        List<MergeTestCase> cases = new ArrayList<>();

        LocalDate baseDate = LocalDate.of(2024, 1, 15);
        String chNubip = "UC_NUBIP";
        String chKpi = "UC_KPI";
        String chKnu = "UC_KNU";

        // Позитивні кейси (повинні об'єднуватися)
        cases.add(new MergeTestCase(
                "Захист дисертації Лабенка Олександра Миколайовича (частина 1)", "Рада ДФ 26.004.001", baseDate, chNubip,
                "Захист дисертації Лабенка Олександра Миколайовича (частина 2)", "Рада ДФ 26.004.001", baseDate, chNubip,
                true, "Кейс 1: НУБіП Лабенко з шаблоном (частина X)"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Лабенка О.М. (частина 1)", "Опис 1", baseDate, chNubip,
                "Захист дисертації Лабенка О.М. (частина 2)", "Опис 2", baseDate, chNubip,
                true, "Кейс 2: НУБіП Лабенко з ініціалами (частина X)"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Шевченка Д.В. ч. 1", "КПІ рада", baseDate, chKpi,
                "Захист дисертації Шевченка Д.В. ч. 2", "КПІ рада", baseDate.plusDays(1),
                chKpi, true, "Кейс 3: Скорочення ч. X з різницею в 1 день"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Петренка П.П. част. 1", "Рада", baseDate, chKnu,
                "Захист дисертації Петренка П.П. част. 2", "Рада", baseDate, chKnu,
                true, "Кейс 4: Скорочення част. X"
        ));

        cases.add(new MergeTestCase(
                "PhD Defense of Ivan Bondarenko part 1", "Thesis topic", baseDate, "UC_CH",
                "PhD Defense of Ivan Bondarenko part 2", "Thesis topic", baseDate, "UC_CH",
                true, "Кейс 5: Англійський маркер part X"
        ));

        cases.add(new MergeTestCase(
                "Публічний захист Коваленка С.П. (part 1)", "Опис", baseDate, chKpi,
                "Публічний захист Коваленка С.П. (part 2)", "Опис", baseDate, chKpi,
                true, "Кейс 6: Англійський маркер у дужках (part X)"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Гриценка І.І. частина 1", "Опис", baseDate, chNubip,
                "Захист дисертації Гриценка І.І. частина 2", "Опис", baseDate.plusDays(2), chNubip,
                true, "Кейс 7: Слово частина без дужок з різницею 2 дні"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Мельника А.С. ЧАСТИНА 1", "Економіка", baseDate, chKnu,
                "Захист дисертації Мельника А.С. частина 2", "Економіка", baseDate, chKnu,
                true, "Кейс 8: Різний регістр слова ЧАСТИНА"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Сидоренка Олександра Васильовича (частина 1)", "Тема", baseDate, chKpi,
                "Захист дисертації Сидоренка О.В. (частина 2)", "Тема", baseDate, chKpi,
                true, "Кейс 9: Повне ім'я в першій частині та ініціали в другій"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Кравчука М.М. ч. 1", "Опис", baseDate, chNubip,
                "Захист дисертації Кравчука М.М. ч. 2", "Опис", baseDate.plusDays(3), chNubip,
                true, "Кейс 10: Гранична дата <= 3 днів"
        ));

        cases.add(new MergeTestCase(
                "Трансляція захисту Василенка В.В. на тему «Методи оптимізації» (частина 1)", "Опис", baseDate, chKpi,
                "Трансляція захисту Василенка В.В. на тему «Методи оптимізації» (частина 2)", "Опис", baseDate, chKpi,
                true, "Кейс 11: Назва з темою дисертації та маркером"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Романенка О.П. (частина 1)", "Здобувач: Романенко Олексій Павлович", baseDate, null,
                "Захист дисертації Романенка О.П. (частина 2)", "Здобувач: Романенко Олексій Павлович", baseDate, null,
                true, "Кейс 12: Без зазначення ідентифікатора каналу"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Ткачука Д.О. ч. 1", "Опис", baseDate, chKnu,
                "Захист дисертації Ткачука Д.О. ч. 3", "Опис", baseDate.plusDays(1), chKnu,
                true, "Кейс 13: Частина 1 і частина 3 одного захисту"
        ));

        cases.add(new MergeTestCase(
                "Засідання спеціалізованої вченої ради. Захист Іванченка І.І. (частина 1)", "Опис", baseDate, chKpi,
                "Засідання спеціалізованої вченої ради. Захист Іванченка І.І. (частина 2)", "Опис", baseDate, chKpi,
                true, "Кейс 14: Схожа довга базова назва"
        ));

        // Негативні кейси (не повинні об'єднуватися)
        cases.add(new MergeTestCase(
                "Захист дисертації Лабенка О.М. (частина 1)", "Опис", baseDate, chNubip,
                "Захист дисертації Шевченка Д.В. (частина 2)", "Опис", baseDate, chNubip,
                false, "Кейс 15: Різні здобувачі на одному каналі"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Лабенка О.М. (частина 1)", "Опис", baseDate, chNubip,
                "Захист дисертації Лабенка О.М. (частина 2)", "Опис", baseDate.plusDays(10), chNubip,
                false, "Кейс 16: Різниця дат 10 днів (> 3 днів)"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Петренка П.П. ч. 1", "Опис", baseDate, chNubip,
                "Захист дисертації Петренка П.П. ч. 2", "Опис", baseDate.plusDays(4), chNubip,
                false, "Кейс 17: Гранична різниця дат 4 дні (> 3 днів)"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Коваленка В.С. (частина 1)", "Опис", baseDate, "UC_CHANNEL_A",
                "Захист дисертації Коваленка В.С. (частина 2)", "Опис", baseDate, "UC_CHANNEL_B",
                false, "Кейс 18: Різні канали публікації"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Шевченка Д.В. на здобуття ступеня доктора філософії", "Опис 1", baseDate, chKpi,
                "Захист дисертації Мельника А.С. на тему «Моделювання стабільності»", "Опис 2", baseDate, chKnu,
                false, "Кейс 19: Абсолютно різні дисертації без маркерів"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Мельника А.С.", "Опис", baseDate, chKnu,
                "Урочисте вручення дипломів доктора філософії", "Опис", baseDate, chKnu,
                false, "Кейс 20: Захист дисертації та інша подія університету"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Коваленка О.В. (частина 1)", "Опис", baseDate, chKpi,
                "Захист дисертації Коваленка І.П. (частина 2)", "Опис", baseDate, chKpi,
                false, "Кейс 21: Однакове прізвище, але різні ініціали"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Бондаренко О.І. (частина 1)", "Опис", baseDate, "UC_LP",
                "Захист дисертації Бондаренко О.І. (частина 2)", "Опис", baseDate.plusMonths(5), "UC_LP",
                false, "Кейс 22: Різниця дат 5 місяців"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Лабенка О.М. (частина 1)", "Опис", baseDate, chNubip,
                "Захист дисертації Петренка П.П. (частина 1)", "Опис", baseDate, chNubip,
                false, "Кейс 23: Обидва відео є першими частинами різних людей"
        ));

        cases.add(new MergeTestCase(
                "Конференція молодих вчених КПІ ім. Ігоря Сікорського ч. 1", "Опис", baseDate, chKpi,
                "Засідання вченої ради факультету ч. 2", "Опис", baseDate, chKpi,
                false, "Кейс 24: Різні заходи без спільних ознак захисту"
        ));

        cases.add(new MergeTestCase(
                "Захист дисертації Гнатюка В.М. (частина 1)", "Тема А", baseDate, chKnu,
                "Захист дисертації Дмитренка С.С. (частина 1)", "Тема Б", baseDate, chKnu,
                false, "Кейс 25: Різні теми та різні автори"
        ));

        return cases;
    }
}

