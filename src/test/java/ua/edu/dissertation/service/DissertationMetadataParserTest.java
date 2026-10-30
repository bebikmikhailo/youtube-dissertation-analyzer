package ua.edu.dissertation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.dissertation.dto.ParsedDissertationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class DissertationMetadataParserTest {

    private DissertationMetadataParser parser;

    @BeforeEach
    void setUp() {
        parser = new DissertationMetadataParser();
    }

    @Test
    @DisplayName("FR-04: Парсинг метаданих захисту Лабенка О.М. у НУБіП (частина 1)")
    void shouldParseLabenkoNubipDefenseMetadata() {
        String title = "Захист дисертації Лабенка О.М. (частина 1)";
        String description = "Разова спеціалізована вчена рада ДФ 26.004.001. Захист дисертації Лабенка Олександра Миколайовича "
                + "на тему «Фінансове забезпечення інноваційного розвитку аграрних підприємств» "
                + "за спеціальністю 072 Фінанси, банківська справа та страхування. НУБіП України.";

        ParsedDissertationMetadata metadata = parser.parse(title, description);

        boolean isAuthorCorrect = "Лабенко Олександр Миколайович".equals(metadata.getAuthorFullName());
        boolean isTopicCorrect = "Фінансове забезпечення інноваційного розвитку аграрних підприємств".equals(metadata.getTopic());
        boolean isCodeCorrect = "072".equals(metadata.getSpecialtyCode());
        boolean isSpecialtyNameCorrect = metadata.getSpecialtyName().contains("Фінанси");
        boolean isCouncilCorrect = metadata.getCouncil().contains("ДФ 26.004.001");
        boolean isUniversityCorrect = "НУБіП України".equals(metadata.getUniversity());

        assertThat(isAuthorCorrect).isTrue();
        assertThat(isTopicCorrect).isTrue();
        assertThat(isCodeCorrect).isTrue();
        assertThat(isSpecialtyNameCorrect).isTrue();
        assertThat(isCouncilCorrect).isTrue();
        assertThat(isUniversityCorrect).isTrue();
    }

    @Test
    @DisplayName("FR-04: Парсинг структурованих метаданих захисту КПІ ім. Ігоря Сікорського")
    void shouldParseKpiStructuredDefenseMetadata() {
        String title = "Публічний захист дисертації Шевченка Д.В. на здобуття ступеня доктора філософії";
        String description = "Спеціалізована вчена рада Д 26.002.01 КПІ ім. Ігоря Сікорського. "
                + "Здобувач: Шевченко Дмитро Володимирович. "
                + "Тема: «Методи машинного навчання для виявлення кібератак у хмарних сховищах». "
                + "Спеціальність: 121 Інженерія програмного забезпечення.";

        ParsedDissertationMetadata metadata = parser.parse(title, description);

        boolean isAuthorCorrect = "Шевченко Дмитро Володимирович".equals(metadata.getAuthorFullName());
        boolean isTopicCorrect = "Методи машинного навчання для виявлення кібератак у хмарних сховищах".equals(metadata.getTopic());
        boolean isCodeCorrect = "121".equals(metadata.getSpecialtyCode());
        boolean isSpecialtyNameCorrect = "Інженерія програмного забезпечення".equals(metadata.getSpecialtyName());
        boolean isCouncilCorrect = metadata.getCouncil().contains("Д 26.002.01");
        boolean isUniversityCorrect = "КПІ ім. Ігоря Сікорського".equals(metadata.getUniversity());

        assertThat(isAuthorCorrect).isTrue();
        assertThat(isTopicCorrect).isTrue();
        assertThat(isCodeCorrect).isTrue();
        assertThat(isSpecialtyNameCorrect).isTrue();
        assertThat(isCouncilCorrect).isTrue();
        assertThat(isUniversityCorrect).isTrue();
    }

    @Test
    @DisplayName("FR-04: Парсинг захисту дисертації КНУ ім. Тараса Шевченка з темою в заголовку")
    void shouldParseKnuDefenseWithTopicInTitle() {
        String title = "Захист дисертації Мельника А.С. на тему «Моделювання макроекономічної стабільності»";
        String description = "Разова вчена рада КНУ імені Тараса Шевченка. Здобувач: Мельник Артем Сергійович. Спеціальність: 051 Економіка.";

        ParsedDissertationMetadata metadata = parser.parse(title, description);

        boolean isAuthorCorrect = "Мельник Артем Сергійович".equals(metadata.getAuthorFullName());
        boolean isTopicCorrect = "Моделювання макроекономічної стабільності".equals(metadata.getTopic());
        boolean isCodeCorrect = "051".equals(metadata.getSpecialtyCode());
        boolean isSpecialtyNameCorrect = "Економіка".equals(metadata.getSpecialtyName());
        boolean isUniversityCorrect = "КНУ імені Тараса Шевченка".equals(metadata.getUniversity());

        assertThat(isAuthorCorrect).isTrue();
        assertThat(isTopicCorrect).isTrue();
        assertThat(isCodeCorrect).isTrue();
        assertThat(isSpecialtyNameCorrect).isTrue();
        assertThat(isUniversityCorrect).isTrue();
    }

    @Test
    @DisplayName("FR-04: Парсинг захисту дисертації Національного університету «Львівська політехніка»")
    void shouldParseLvivPolytechnicDefenseMetadata() {
        String title = "Захист дисертації Бондаренко О.І. на тему «Архітектура розподілених інтелектуальних систем»";
        String description = "Спеціалізована вчена рада Національного університету «Львівська політехніка». "
                + "Здобувач: Бондаренко Олена Ігорівна. Спеціальність: 122 Комп'ютерні науки.";

        ParsedDissertationMetadata metadata = parser.parse(title, description);

        boolean isAuthorCorrect = "Бондаренко Олена Ігорівна".equals(metadata.getAuthorFullName());
        boolean isTopicCorrect = "Архітектура розподілених інтелектуальних систем".equals(metadata.getTopic());
        boolean isCodeCorrect = "122".equals(metadata.getSpecialtyCode());
        boolean isSpecialtyNameCorrect = metadata.getSpecialtyName().contains("Комп'ютерні науки");
        boolean isUniversityCorrect = metadata.getUniversity().contains("Львівська політехніка");

        assertThat(isAuthorCorrect).isTrue();
        assertThat(isTopicCorrect).isTrue();
        assertThat(isCodeCorrect).isTrue();
        assertThat(isSpecialtyNameCorrect).isTrue();
        assertThat(isUniversityCorrect).isTrue();
    }

    @Test
    @DisplayName("FR-04: Нормалізація ПІБ здобувачів з родового відмінка у називний")
    void shouldNormalizeVariousGenitiveNamesToNominative() {
        String labenkoFull = parser.normalizeAuthorNameToNominative("Лабенка Олександра Миколайовича");
        String labenkoInitials = parser.normalizeAuthorNameToNominative("Лабенка О.М.");
        String shevchenkoFull = parser.normalizeAuthorNameToNominative("Шевченка Дмитра Володимировича");
        String melnykFull = parser.normalizeAuthorNameToNominative("Мельника Артема Сергійовича");
        String bondarenkoFemale = parser.normalizeAuthorNameToNominative("Бондаренко Олени Ігорівни");

        boolean isLabenkoFullCorrect = "Лабенко Олександр Миколайович".equals(labenkoFull);
        boolean isLabenkoInitialsCorrect = "Лабенко О.М.".equals(labenkoInitials);
        boolean isShevchenkoFullCorrect = "Шевченко Дмитро Володимирович".equals(shevchenkoFull);
        boolean isMelnykFullCorrect = "Мельник Артем Сергійович".equals(melnykFull);
        boolean isBondarenkoFemaleCorrect = "Бондаренко Олена Ігорівна".equals(bondarenkoFemale);

        assertThat(isLabenkoFullCorrect).isTrue();
        assertThat(isLabenkoInitialsCorrect).isTrue();
        assertThat(isShevchenkoFullCorrect).isTrue();
        assertThat(isMelnykFullCorrect).isTrue();
        assertThat(isBondarenkoFemaleCorrect).isTrue();
    }
}

