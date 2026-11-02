package ua.edu.dissertation.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.MultiPartExportMode;
import ua.edu.dissertation.dto.ReportFormat;
import ua.edu.dissertation.dto.VideoPartDetailDto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TC_FR_16_ReportExportTest {

    private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Autowired
    private MockMvc mockMvc;

    private List<DefenseAggregationDto> sampleDefenses;

    @BeforeEach
    void setUp() {
        VideoPartDetailDto part1 = VideoPartDetailDto.builder()
                .videoId(101L)
                .youtubeVideoId("yt-lab-1")
                .partNumber(1)
                .title("Захист дисертації Лабенка О. М. (частина 1)")
                .publishedAt(LocalDate.of(2023, 10, 15))
                .durationSeconds(7200)
                .formattedDuration("2 год 00 хв")
                .viewCount(256)
                .likeCount(18)
                .commentCount(4)
                .channelName("НУБіП України")
                .build();

        VideoPartDetailDto part2 = VideoPartDetailDto.builder()
                .videoId(102L)
                .youtubeVideoId("yt-lab-2")
                .partNumber(2)
                .title("Захист дисертації Лабенка О. М. (частина 2)")
                .publishedAt(LocalDate.of(2023, 10, 15))
                .durationSeconds(3600)
                .formattedDuration("1 год 00 хв")
                .viewCount(115)
                .likeCount(8)
                .commentCount(2)
                .channelName("НУБіП України")
                .build();

        DefenseAggregationDto multiPartDefense = DefenseAggregationDto.builder()
                .dissertationId(1L)
                .authorFullName("Лабенко Олександр Миколайович")
                .topic("Управління інтелектуальними агросистемами")
                .specialty("051 Економіка")
                .councilName("Д 26.004.01")
                .universityName("НУБіП України")
                .totalPartsCount(2)
                .totalViewCount(371)
                .totalLikeCount(26)
                .totalCommentCount(6)
                .totalDurationSeconds(10800)
                .formattedTotalDuration("3 год 00 хв")
                .parts(List.of(part1, part2))
                .build();

        VideoPartDetailDto singlePart = VideoPartDetailDto.builder()
                .videoId(201L)
                .youtubeVideoId("yt-kov-1")
                .partNumber(1)
                .title("Захист дисертації Ковальчука С. П.")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(5400)
                .formattedDuration("1 год 30 хв")
                .viewCount(420)
                .likeCount(30)
                .commentCount(9)
                .channelName("КПІ ім. Ігоря Сікорського")
                .build();

        DefenseAggregationDto singlePartDefense = DefenseAggregationDto.builder()
                .dissertationId(2L)
                .authorFullName("Ковальчук Сергій Петрович")
                .topic("Розробка методів оптимізації мікросервісних систем")
                .specialty("121 Інженерія програмного забезпечення")
                .councilName("Д 26.002.02")
                .universityName("КПІ ім. Ігоря Сікорського")
                .totalPartsCount(1)
                .totalViewCount(420)
                .totalLikeCount(30)
                .totalCommentCount(9)
                .totalDurationSeconds(5400)
                .formattedTotalDuration("1 год 30 хв")
                .parts(List.of(singlePart))
                .build();

        sampleDefenses = List.of(multiPartDefense, singlePartDefense);
    }

    @Test
    @DisplayName("FR-16: Експорт Excel у Режимі 1 (тільки сумарно) формує три аркуші та сумарні рядки")
    void shouldExportExcelInSummaryOnlyMode() throws IOException {
        byte[] excelBytes = reportGeneratorService.exportExcel(sampleDefenses, MultiPartExportMode.SUMMARY_ONLY);
        assertThat(excelBytes).isNotNull().isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
            assertThat(workbook.getSheetName(0)).isEqualTo("Захисти дисертацій");
            assertThat(workbook.getSheetName(1)).isEqualTo("Статистичний аналіз");
            assertThat(workbook.getSheetName(2)).isEqualTo("Порівняння ЗВО");

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(2);
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Здобувач (ПІБ)");
            assertThat(headerRow.getCell(9).getStringCellValue()).isEqualTo("Сумарні перегляди");

            Row row1 = sheet.getRow(3);
            assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("Лабенко Олександр Миколайович");
            assertThat((int) row1.getCell(8).getNumericCellValue()).isEqualTo(2);
            assertThat((int) row1.getCell(9).getNumericCellValue()).isEqualTo(371);

            Row row2 = sheet.getRow(4);
            assertThat(row2.getCell(2).getStringCellValue()).isEqualTo("Ковальчук Сергій Петрович");
            assertThat((int) row2.getCell(8).getNumericCellValue()).isEqualTo(1);
            assertThat((int) row2.getCell(9).getNumericCellValue()).isEqualTo(420);

            boolean hasExtraRows = sheet.getRow(5) != null;
            assertThat(hasExtraRows).isFalse();
        }
    }

    @Test
    @DisplayName("FR-16: Експорт Excel у Режимі 2 (окремо по частинах) формує рядки для кожної частини відео")
    void shouldExportExcelInPartsOnlyMode() throws IOException {
        byte[] excelBytes = reportGeneratorService.exportExcel(sampleDefenses, MultiPartExportMode.PARTS_ONLY);
        assertThat(excelBytes).isNotNull().isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Захисти дисертацій");
            Row headerRow = sheet.getRow(2);
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Частина");
            assertThat(headerRow.getCell(8).getStringCellValue()).isEqualTo("Перегляди");

            Row partRow1 = sheet.getRow(3);
            assertThat(partRow1.getCell(2).getStringCellValue()).isEqualTo("Лабенко Олександр Миколайович");
            assertThat(partRow1.getCell(3).getStringCellValue()).isEqualTo("Частина 1");
            assertThat((int) partRow1.getCell(8).getNumericCellValue()).isEqualTo(256);

            Row partRow2 = sheet.getRow(4);
            assertThat(partRow2.getCell(2).getStringCellValue()).isEqualTo("Лабенко Олександр Миколайович");
            assertThat(partRow2.getCell(3).getStringCellValue()).isEqualTo("Частина 2");
            assertThat((int) partRow2.getCell(8).getNumericCellValue()).isEqualTo(115);

            Row partRow3 = sheet.getRow(5);
            assertThat(partRow3.getCell(2).getStringCellValue()).isEqualTo("Ковальчук Сергій Петрович");
            assertThat(partRow3.getCell(3).getStringCellValue()).isEqualTo("Частина 1");
            assertThat((int) partRow3.getCell(8).getNumericCellValue()).isEqualTo(420);
        }
    }

    @Test
    @DisplayName("FR-16: Експорт Excel у Режимі 3 (обидва варіанти) містить сумарні та вкладені рядки")
    void shouldExportExcelInBothMode() throws IOException {
        byte[] excelBytes = reportGeneratorService.exportExcel(sampleDefenses, MultiPartExportMode.BOTH);
        assertThat(excelBytes).isNotNull().isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Захисти дисертацій");

            Row summaryRow1 = sheet.getRow(3);
            assertThat(summaryRow1.getCell(1).getStringCellValue()).contains("Сумарно");
            assertThat(summaryRow1.getCell(3).getStringCellValue()).isEqualTo("Лабенко Олександр Миколайович");
            assertThat((int) summaryRow1.getCell(7).getNumericCellValue()).isEqualTo(371);

            Row subRow1 = sheet.getRow(4);
            assertThat(subRow1.getCell(1).getStringCellValue()).contains("Частина 1");
            assertThat((int) subRow1.getCell(7).getNumericCellValue()).isEqualTo(256);

            Row subRow2 = sheet.getRow(5);
            assertThat(subRow2.getCell(1).getStringCellValue()).contains("Частина 2");
            assertThat((int) subRow2.getCell(7).getNumericCellValue()).isEqualTo(115);

            Row summaryRow2 = sheet.getRow(6);
            assertThat(summaryRow2.getCell(1).getStringCellValue()).contains("Сумарно");
            assertThat(summaryRow2.getCell(3).getStringCellValue()).isEqualTo("Ковальчук Сергій Петрович");
        }
    }

    @Test
    @DisplayName("FR-16: Аркуші «Статистичний аналіз» та «Порівняння ЗВО» містять коректні аналітичні блоки")
    void shouldContainAnalyticsAndCouncilComparisonSheets() throws IOException {
        byte[] excelBytes = reportGeneratorService.exportExcel(sampleDefenses, MultiPartExportMode.SUMMARY_ONLY);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet statsSheet = workbook.getSheet("Статистичний аналіз");
            assertThat(statsSheet).isNotNull();
            Row statsTitle = statsSheet.getRow(0);
            assertThat(statsTitle.getCell(0).getStringCellValue()).contains("Статистичний аналіз");

            Sheet comparisonSheet = workbook.getSheet("Порівняння ЗВО");
            assertThat(comparisonSheet).isNotNull();
            Row compTitle = comparisonSheet.getRow(0);
            assertThat(compTitle.getCell(0).getStringCellValue()).contains("Порівняння подання інформації");
        }
    }

    @Test
    @DisplayName("FR-16: Експорт PDF у всіх трьох режимах генерує коректний документ")
    void shouldExportPdfInAllThreeModes() {
        for (MultiPartExportMode mode : MultiPartExportMode.values()) {
            byte[] pdfBytes = reportGeneratorService.exportPDF(sampleDefenses, mode);
            assertThat(pdfBytes).isNotNull().isNotEmpty();
            assertThat(pdfBytes.length).isGreaterThan(1000);

            String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8));
            assertThat(pdfHeader).startsWith("%PDF-");
        }
    }

    @Test
    @DisplayName("FR-16: createReport підтримує поліморфний вибір між форматами Excel та PDF")
    void shouldCreateReportBasedOnRequestedFormat() {
        byte[] excel = reportGeneratorService.createReport(ReportFormat.EXCEL, sampleDefenses, MultiPartExportMode.BOTH);
        assertThat(excel).isNotNull().isNotEmpty();

        byte[] pdf = reportGeneratorService.createReport(ReportFormat.PDF, sampleDefenses, MultiPartExportMode.BOTH);
        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("FR-16: Обробка порожнього списку захистів виконується без винятків")
    void shouldHandleEmptyDefensesGracefully() {
        byte[] excelBytes = reportGeneratorService.exportExcel(List.of(), MultiPartExportMode.SUMMARY_ONLY);
        assertThat(excelBytes).isNotNull().isNotEmpty();

        byte[] pdfBytes = reportGeneratorService.exportPDF(List.of(), MultiPartExportMode.SUMMARY_ONLY);
        assertThat(pdfBytes).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("FR-16: Веб-ендпоінти експорту повертають HTTP 200 та файлові заголовки")
    void shouldProvideDownloadEndpointsInWebMvc() throws Exception {
        mockMvc.perform(get("/reports/excel").param("mode", "SUMMARY_ONLY"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE)))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dissertations_report_summary_only.xlsx\""));

        mockMvc.perform(get("/reports/pdf").param("mode", "BOTH"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dissertations_report_both.pdf\""));

        mockMvc.perform(get("/defenses/export/excel").param("mode", "PARTS_ONLY"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dissertations_report_parts_only.xlsx\""));
    }
}

