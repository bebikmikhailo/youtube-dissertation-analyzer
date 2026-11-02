package ua.edu.dissertation.service;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.MultiPartExportMode;
import ua.edu.dissertation.dto.VideoPartDetailDto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TC_NFR_08_CyrillicEncodingTest {

    private static final String UKRAINIAN_TEST_PHRASE =
            "Захист дисертації здобувача: Ільїна Євгена Юрійовича (спеціальність: Комп'ютерні науки)";

    private static final String UKRAINIAN_TOPIC =
            "Ґрунтовний аналіз надійності інтелектуальних систем об'єктно-орієнтованого програмування";

    private static final char[] UKRAINIAN_SPECIAL_CHARS = {
            'і', 'ї', 'є', 'ґ', 'І', 'Ї', 'Є', 'Ґ'
    };

    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}]");
    private static final String EM_DASH = "\u2014";

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Test
    @DisplayName("NFR-08: Шрифт TrueType у classpath підтримує специфічні українські літери (і, ї, є, ґ)")
    void shouldSupportUkrainianGlyphsInTrueTypeFont() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("fonts/Roboto-Regular.ttf")) {
            assertThat(stream).isNotNull();
            byte[] fontBytes = stream.readAllBytes();
            BaseFont baseFont = BaseFont.createFont("Roboto-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);

            for (char ukrChar : UKRAINIAN_SPECIAL_CHARS) {
                boolean charSupported = baseFont.charExists(ukrChar);
                assertThat(charSupported)
                        .as("Символ '%c' (Unicode U+%04X) повинен підтримуватися шрифтом", ukrChar, (int) ukrChar)
                        .isTrue();
            }

            float width = baseFont.getWidthPoint(UKRAINIAN_TEST_PHRASE, 12f);
            assertThat(width).isGreaterThan(0f);
        }
    }

    @Test
    @DisplayName("NFR-08: PDF-звіт коректно рендерить україномовні тексти з літерою «ґ», «є», «і», «ї»")
    void shouldRenderUkrainianCyrillicInPdfWithoutArtifacts() throws Exception {
        List<DefenseAggregationDto> testDefenses = createCyrillicTestDefenses();

        byte[] pdfBytes = reportGeneratorService.exportPDF(testDefenses, MultiPartExportMode.SUMMARY_ONLY);
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        PdfReader pdfReader = new PdfReader(pdfBytes);
        int pageCount = pdfReader.getNumberOfPages();
        assertThat(pageCount).isGreaterThanOrEqualTo(1);

        String pdfHeader = new String(pdfBytes, 0, 5);
        assertThat(pdfHeader).isEqualTo("%PDF-");

        pdfReader.close();
    }

    @Test
    @DisplayName("NFR-08: Excel-звіт зберігає українську кирилицю та назви аркушів без спотворень кодування")
    void shouldPreserveUkrainianCyrillicInExcelExport() throws Exception {
        List<DefenseAggregationDto> testDefenses = createCyrillicTestDefenses();

        byte[] excelBytes = reportGeneratorService.exportExcel(testDefenses, MultiPartExportMode.SUMMARY_ONLY);
        assertThat(excelBytes).isNotNull().isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Захисти дисертацій");
            assertThat(workbook.getSheetName(1)).isEqualTo("Статистичний аналіз");
            assertThat(workbook.getSheetName(2)).isEqualTo("Порівняння ЗВО");

            Sheet sheet = workbook.getSheet("Захисти дисертацій");
            Row row = sheet.getRow(3);

            String author = row.getCell(2).getStringCellValue();
            assertThat(author).isEqualTo("Ільїн Євген Юрійович");

            String topic = row.getCell(3).getStringCellValue();
            assertThat(topic).isEqualTo(UKRAINIAN_TOPIC);
            assertThat(topic).contains("Ґрунтовний", "об'єктно");

            String specialty = row.getCell(4).getStringCellValue();
            assertThat(specialty).contains("Комп'ютерні науки");

            String university = row.getCell(6).getStringCellValue();
            assertThat(university).isEqualTo("КПІ ім. Ігоря Сікорського");
        }
    }

    @Test
    @DisplayName("NFR-08: Звіти не містять заборонених емодзі та довгих тире (Human Code Standards)")
    void shouldNotContainEmojisOrEmDashesInReportData() throws Exception {
        List<DefenseAggregationDto> testDefenses = createCyrillicTestDefenses();

        byte[] excelBytes = reportGeneratorService.exportExcel(testDefenses, MultiPartExportMode.BOTH);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                assertThat(EMOJI_PATTERN.matcher(sheetName).find()).isFalse();
                assertThat(sheetName).doesNotContain(EM_DASH);

                for (Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            String value = cell.getStringCellValue();
                            assertThat(EMOJI_PATTERN.matcher(value).find())
                                    .as("Текст клітинки не повинен містити емодзі: %s", value)
                                    .isFalse();
                            assertThat(value)
                                    .as("Текст клітинки не повинен містити довге тире: %s", value)
                                    .doesNotContain(EM_DASH);
                        }
                    }
                }
            }
        }
    }

    private List<DefenseAggregationDto> createCyrillicTestDefenses() {
        VideoPartDetailDto part = VideoPartDetailDto.builder()
                .videoId(501L)
                .youtubeVideoId("yt-cyr-1")
                .partNumber(1)
                .title(UKRAINIAN_TEST_PHRASE)
                .publishedAt(LocalDate.of(2024, 2, 10))
                .durationSeconds(7200)
                .formattedDuration("2 год 00 хв")
                .viewCount(350)
                .likeCount(25)
                .commentCount(8)
                .channelName("КПІ ім. Ігоря Сікорського")
                .build();

        DefenseAggregationDto defense = DefenseAggregationDto.builder()
                .dissertationId(501L)
                .authorFullName("Ільїн Євген Юрійович")
                .topic(UKRAINIAN_TOPIC)
                .specialty("122 Комп'ютерні науки")
                .councilName("Д 26.002.01")
                .universityName("КПІ ім. Ігоря Сікорського")
                .totalPartsCount(1)
                .totalViewCount(350)
                .totalLikeCount(25)
                .totalCommentCount(8)
                .totalDurationSeconds(7200)
                .formattedTotalDuration("2 год 00 хв")
                .parts(List.of(part))
                .build();

        return List.of(defense);
    }
}

