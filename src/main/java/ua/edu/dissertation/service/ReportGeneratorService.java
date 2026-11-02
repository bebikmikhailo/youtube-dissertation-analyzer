package ua.edu.dissertation.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.dissertation.dto.ChannelRatingDto;
import ua.edu.dissertation.dto.CorrelationAnalysisDto;
import ua.edu.dissertation.dto.CorrelationResultDto;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.DescriptiveStatisticsDto;
import ua.edu.dissertation.dto.MetricType;
import ua.edu.dissertation.dto.MultiPartExportMode;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.ReportFormat;
import ua.edu.dissertation.dto.SpecialtyRatingDto;
import ua.edu.dissertation.dto.UniversityPresentationProfile;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.VideoPartDetailDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ReportGeneratorService implements ReportGenerator {

    private static final String ROBOTO_REGULAR_RESOURCE = "fonts/Roboto-Regular.ttf";
    private static final String ROBOTO_BOLD_RESOURCE = "fonts/Roboto-Bold.ttf";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AnalyzerService analyzerService;
    private final DefenseCatalogService defenseCatalogService;

    private BaseFont cachedRegularFont;
    private BaseFont cachedBoldFont;

    public ReportGeneratorService(AnalyzerService analyzerService,
                                  DefenseCatalogService defenseCatalogService) {
        this.analyzerService = analyzerService;
        this.defenseCatalogService = defenseCatalogService;
    }

    @Override
    public byte[] createReport(ReportFormat format, List<DefenseAggregationDto> defenses, MultiPartExportMode mode) {
        ReportFormat effectiveFormat = format != null ? format : ReportFormat.EXCEL;
        boolean isPdf = effectiveFormat == ReportFormat.PDF;
        return isPdf ? exportPDF(defenses, mode) : exportExcel(defenses, mode);
    }

    public byte[] createReport(ReportFormat format, MultiPartExportMode mode) {
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(new VideoFilterCriteria());
        return createReport(format, defenses, mode);
    }

    public byte[] exportExcel(MultiPartExportMode mode) {
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(new VideoFilterCriteria());
        return exportExcel(defenses, mode);
    }

    public byte[] exportExcel(VideoFilterCriteria criteria, MultiPartExportMode mode) {
        VideoFilterCriteria effectiveCriteria = criteria != null ? criteria : new VideoFilterCriteria();
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(effectiveCriteria);
        return exportExcel(defenses, mode);
    }

    @Override
    public byte[] exportExcel(List<DefenseAggregationDto> defenses, MultiPartExportMode mode) {
        MultiPartExportMode effectiveMode = mode != null ? mode : MultiPartExportMode.SUMMARY_ONLY;
        List<DefenseAggregationDto> effectiveDefenses = defenses != null ? defenses : List.of();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            ExcelStyles styles = createExcelStyles(workbook);

            buildExcelDefensesSheet(workbook, styles, effectiveDefenses, effectiveMode);
            buildExcelStatisticsSheet(workbook, styles);
            buildExcelCouncilComparisonSheet(workbook, styles);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("Помилка формування книги Excel", exception);
            throw new IllegalStateException("Не вдалося сформувати Excel-звіт", exception);
        }
    }

    public byte[] exportPDF(MultiPartExportMode mode) {
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(new VideoFilterCriteria());
        return exportPDF(defenses, mode);
    }

    public byte[] exportPDF(VideoFilterCriteria criteria, MultiPartExportMode mode) {
        VideoFilterCriteria effectiveCriteria = criteria != null ? criteria : new VideoFilterCriteria();
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(effectiveCriteria);
        return exportPDF(defenses, mode);
    }

    @Override
    public byte[] exportPDF(List<DefenseAggregationDto> defenses, MultiPartExportMode mode) {
        MultiPartExportMode effectiveMode = mode != null ? mode : MultiPartExportMode.SUMMARY_ONLY;
        List<DefenseAggregationDto> effectiveDefenses = defenses != null ? defenses : List.of();

        BaseFont regularFont = getRegularFont();
        BaseFont boldFont = getBoldFont();

        Font titleFont = new Font(boldFont, 16, Font.BOLD, new Color(23, 32, 51));
        Font subtitleFont = new Font(regularFont, 9, Font.NORMAL, new Color(108, 117, 125));
        Font sectionHeadingFont = new Font(boldFont, 12, Font.BOLD, new Color(23, 32, 51));
        Font tableHeaderFont = new Font(boldFont, 8, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(regularFont, 8, Font.NORMAL, new Color(43, 47, 54));
        Font cellBoldFont = new Font(boldFont, 8, Font.BOLD, new Color(43, 47, 54));
        Font cellSmallFont = new Font(regularFont, 7, Font.NORMAL, new Color(108, 117, 125));
        Font textFont = new Font(regularFont, 9, Font.NORMAL, new Color(43, 47, 54));
        Font boldTextFont = new Font(boldFont, 9, Font.BOLD, new Color(43, 47, 54));

        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PdfFooterEvent(regularFont));
            document.open();

            Paragraph headerCategory = new Paragraph("СИСТЕМА АНАЛІЗУ ПЕРЕГЛЯДІВ ЗАХИСТІВ ДИСЕРТАЦІЙ НА YOUTUBE", subtitleFont);
            headerCategory.setSpacingAfter(2f);
            document.add(headerCategory);

            Paragraph reportTitle = new Paragraph("Аналітичний звіт щодо публікацій захистів дисертацій", titleFont);
            reportTitle.setSpacingAfter(4f);
            document.add(reportTitle);

            String metaLine = "Дата формування: " + LocalDateTime.now().format(DATETIME_FORMATTER)
                    + "  |  Режим вивантаження: " + effectiveMode.getDescription();
            Paragraph metaParagraph = new Paragraph(metaLine, subtitleFont);
            metaParagraph.setSpacingAfter(12f);
            document.add(metaParagraph);

            document.add(createPdfKpiTable(effectiveDefenses, tableHeaderFont, cellFont, cellBoldFont));

            Paragraph sectionDefenses = new Paragraph("1. Реєстр захистів дисертацій (" + effectiveMode.getDescription() + ")", sectionHeadingFont);
            sectionDefenses.setSpacingBefore(12f);
            sectionDefenses.setSpacingAfter(6f);
            document.add(sectionDefenses);

            document.add(createPdfDefensesTable(effectiveDefenses, effectiveMode, tableHeaderFont, cellFont, cellBoldFont, cellSmallFont));

            Paragraph sectionStats = new Paragraph("2. Статистичний аналіз та кореляції показників", sectionHeadingFont);
            sectionStats.setSpacingBefore(14f);
            sectionStats.setSpacingAfter(6f);
            document.add(sectionStats);

            document.add(createPdfDescriptiveStatisticsTable(tableHeaderFont, cellFont, cellBoldFont));

            Paragraph correlationHeading = new Paragraph("Кореляційний аналіз факторів переглядів (коефіцієнт Пірсона r):", boldTextFont);
            correlationHeading.setSpacingBefore(8f);
            correlationHeading.setSpacingAfter(4f);
            document.add(correlationHeading);

            document.add(createPdfCorrelationTable(tableHeaderFont, cellFont, cellBoldFont));

            Paragraph sectionComparison = new Paragraph("3. Порівняння подання інформації радами топ-ЗВО України (FR-14)", sectionHeadingFont);
            sectionComparison.setSpacingBefore(14f);
            sectionComparison.setSpacingAfter(6f);
            document.add(sectionComparison);

            PresentationComparisonReport comparisonReport = analyzerService.compareCouncils();
            document.add(createPdfCouncilComparisonTable(comparisonReport, tableHeaderFont, cellFont, cellBoldFont));

            buildPdfCouncilComparisonText(document, comparisonReport, boldTextFont, textFont);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            log.error("Помилка формування PDF-звіту", exception);
            throw new IllegalStateException("Не вдалося сформувати PDF-звіт", exception);
        }
    }

    private void buildExcelDefensesSheet(XSSFWorkbook workbook,
                                         ExcelStyles styles,
                                         List<DefenseAggregationDto> defenses,
                                         MultiPartExportMode mode) {
        Sheet sheet = workbook.createSheet("Захисти дисертацій");

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Реєстр захистів дисертацій на YouTube - " + mode.getDescription());
        titleCell.setCellStyle(styles.sheetTitleStyle);

        Row emptyRow = sheet.createRow(rowIndex++);
        Row headerRow = sheet.createRow(rowIndex++);

        boolean isSummaryOnly = mode == MultiPartExportMode.SUMMARY_ONLY;
        boolean isPartsOnly = mode == MultiPartExportMode.PARTS_ONLY;

        if (isSummaryOnly) {
            String[] headers = {
                    "№", "ID дисертації", "Здобувач (ПІБ)", "Тема дисертації", "Спеціальність",
                    "Вчена рада", "Заклад вищої освіти", "Канал YouTube", "Кількість частин",
                    "Сумарні перегляди", "Сумарні лайки", "Сумарні коментарі", "Сумарна тривалість (с)",
                    "Тривалість", "Дата публікації"
            };
            populateExcelHeaders(headerRow, headers, styles.headerStyle);

            int itemIndex = 1;
            for (DefenseAggregationDto defense : defenses) {
                Row row = sheet.createRow(rowIndex++);
                writeExcelCell(row, 0, itemIndex++, styles.integerStyle);
                writeExcelCell(row, 1, defense.getDissertationId(), styles.integerStyle);
                writeExcelCell(row, 2, defense.getAuthorFullName(), styles.textStyle);
                writeExcelCell(row, 3, defense.getTopic(), styles.textStyle);
                writeExcelCell(row, 4, defense.getSpecialty(), styles.textStyle);
                writeExcelCell(row, 5, defense.getCouncilName(), styles.textStyle);
                writeExcelCell(row, 6, defense.getUniversityName(), styles.textStyle);
                writeExcelCell(row, 7, defense.getChannelName(), styles.textStyle);
                writeExcelCell(row, 8, defense.getTotalPartsCount(), styles.integerStyle);
                writeExcelCell(row, 9, defense.getTotalViewCount(), styles.integerStyle);
                writeExcelCell(row, 10, defense.getTotalLikeCount(), styles.integerStyle);
                writeExcelCell(row, 11, defense.getTotalCommentCount(), styles.integerStyle);
                writeExcelCell(row, 12, defense.getTotalDurationSeconds(), styles.integerStyle);
                writeExcelCell(row, 13, defense.getFormattedTotalDuration(), styles.textStyle);
                writeExcelCell(row, 14, formatLocalDate(defense.getFirstPublishedAt()), styles.textStyle);
            }
            autoSizeExcelColumns(sheet, headers.length);
        } else if (isPartsOnly) {
            String[] headers = {
                    "№", "ID дисертації", "Здобувач (ПІБ)", "Частина", "Назва відео",
                    "YouTube ID", "Канал YouTube", "Дата публікації", "Перегляди",
                    "Лайки", "Коментарі", "Тривалість (с)", "Тривалість", "Частка переглядів (%)"
            };
            populateExcelHeaders(headerRow, headers, styles.headerStyle);

            int itemIndex = 1;
            for (DefenseAggregationDto defense : defenses) {
                List<VideoPartDetailDto> parts = getDefensePartsOrFallback(defense);
                for (VideoPartDetailDto part : parts) {
                    Row row = sheet.createRow(rowIndex++);
                    writeExcelCell(row, 0, itemIndex++, styles.integerStyle);
                    writeExcelCell(row, 1, defense.getDissertationId(), styles.integerStyle);
                    writeExcelCell(row, 2, defense.getAuthorFullName(), styles.textStyle);
                    writeExcelCell(row, 3, "Частина " + part.getPartNumber(), styles.textStyle);
                    writeExcelCell(row, 4, part.getTitle(), styles.textStyle);
                    writeExcelCell(row, 5, part.getYoutubeVideoId(), styles.textStyle);
                    writeExcelCell(row, 6, part.getChannelName(), styles.textStyle);
                    writeExcelCell(row, 7, formatLocalDate(part.getPublishedAt()), styles.textStyle);
                    writeExcelCell(row, 8, part.getViewCount(), styles.integerStyle);
                    writeExcelCell(row, 9, part.getLikeCount(), styles.integerStyle);
                    writeExcelCell(row, 10, part.getCommentCount(), styles.integerStyle);
                    writeExcelCell(row, 11, part.getDurationSeconds(), styles.integerStyle);
                    writeExcelCell(row, 12, part.getFormattedDuration(), styles.textStyle);
                    writeExcelCell(row, 13, defense.calculateViewShare(part) / 100.0, styles.percentStyle);
                }
            }
            autoSizeExcelColumns(sheet, headers.length);
        } else {
            String[] headers = {
                    "№", "Рівень запису", "ID / Відеокод", "Здобувач (ПІБ)", "Тема / Назва відео",
                    "Спеціальність / Канал", "Дата публікації", "Перегляди", "Лайки",
                    "Коментарі", "Тривалість", "Частка переглядів (%)"
            };
            populateExcelHeaders(headerRow, headers, styles.headerStyle);

            int itemIndex = 1;
            for (DefenseAggregationDto defense : defenses) {
                Row summaryRow = sheet.createRow(rowIndex++);
                writeExcelCell(summaryRow, 0, itemIndex++, styles.summaryRowStyle);
                writeExcelCell(summaryRow, 1, "[Сумарно по захисту]", styles.summaryRowStyle);
                writeExcelCell(summaryRow, 2, defense.getDissertationId(), styles.summaryRowStyle);
                writeExcelCell(summaryRow, 3, defense.getAuthorFullName(), styles.summaryRowStyle);
                writeExcelCell(summaryRow, 4, defense.getTopic(), styles.summaryRowStyle);
                writeExcelCell(summaryRow, 5, defense.getSpecialty(), styles.summaryRowStyle);
                writeExcelCell(summaryRow, 6, formatLocalDate(defense.getFirstPublishedAt()), styles.summaryRowStyle);
                writeExcelCell(summaryRow, 7, defense.getTotalViewCount(), styles.summaryIntegerStyle);
                writeExcelCell(summaryRow, 8, defense.getTotalLikeCount(), styles.summaryIntegerStyle);
                writeExcelCell(summaryRow, 9, defense.getTotalCommentCount(), styles.summaryIntegerStyle);
                writeExcelCell(summaryRow, 10, defense.getFormattedTotalDuration(), styles.summaryRowStyle);
                writeExcelCell(summaryRow, 11, 1.0, styles.summaryPercentStyle);

                List<VideoPartDetailDto> parts = getDefensePartsOrFallback(defense);
                for (VideoPartDetailDto part : parts) {
                    Row partRow = sheet.createRow(rowIndex++);
                    writeExcelCell(partRow, 0, "", styles.subRowStyle);
                    writeExcelCell(partRow, 1, "   -> Частина " + part.getPartNumber(), styles.subRowStyle);
                    writeExcelCell(partRow, 2, part.getYoutubeVideoId(), styles.subRowStyle);
                    writeExcelCell(partRow, 3, defense.getAuthorFullName(), styles.subRowStyle);
                    writeExcelCell(partRow, 4, part.getTitle(), styles.subRowStyle);
                    writeExcelCell(partRow, 5, part.getChannelName(), styles.subRowStyle);
                    writeExcelCell(partRow, 6, formatLocalDate(part.getPublishedAt()), styles.subRowStyle);
                    writeExcelCell(partRow, 7, part.getViewCount(), styles.integerStyle);
                    writeExcelCell(partRow, 8, part.getLikeCount(), styles.integerStyle);
                    writeExcelCell(partRow, 9, part.getCommentCount(), styles.integerStyle);
                    writeExcelCell(partRow, 10, part.getFormattedDuration(), styles.subRowStyle);
                    writeExcelCell(partRow, 11, defense.calculateViewShare(part) / 100.0, styles.percentStyle);
                }
            }
            autoSizeExcelColumns(sheet, headers.length);
        }

        sheet.createFreezePane(0, 3);
    }

    private void buildExcelStatisticsSheet(XSSFWorkbook workbook, ExcelStyles styles) {
        Sheet sheet = workbook.createSheet("Статистичний аналіз");

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Статистичний аналіз переглядів та показників захистів дисертацій");
        titleCell.setCellStyle(styles.sheetTitleStyle);

        sheet.createRow(rowIndex++);

        Row section1Row = sheet.createRow(rowIndex++);
        Cell s1Cell = section1Row.createCell(0);
        s1Cell.setCellValue("1. Дескриптивна статистика вибірки");
        s1Cell.setCellStyle(styles.sectionTitleStyle);

        Row descHeaderRow = sheet.createRow(rowIndex++);
        String[] descHeaders = {"Метрика", "Середнє значення", "Медіана", "Мінімум", "Максимум", "Сума"};
        populateExcelHeaders(descHeaderRow, descHeaders, styles.headerStyle);

        for (MetricRow item : getMetricRows()) {
            DescriptiveStatisticsDto stats = item.stats();
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, item.name(), styles.textStyle);
            writeExcelCell(row, 1, stats != null ? stats.getAverage() : 0.0, styles.decimalStyle);
            writeExcelCell(row, 2, stats != null ? stats.getMedian() : 0.0, styles.decimalStyle);
            writeExcelCell(row, 3, stats != null ? (long) stats.getMin() : 0L, styles.integerStyle);
            writeExcelCell(row, 4, stats != null ? (long) stats.getMax() : 0L, styles.integerStyle);
            writeExcelCell(row, 5, stats != null ? (long) stats.getSum() : 0L, styles.integerStyle);
        }

        sheet.createRow(rowIndex++);

        Row section2Row = sheet.createRow(rowIndex++);
        Cell s2Cell = section2Row.createCell(0);
        s2Cell.setCellValue("2. Кореляційний аналіз факторів впливу на перегляди (FR-11)");
        s2Cell.setCellStyle(styles.sectionTitleStyle);

        Row corrHeaderRow = sheet.createRow(rowIndex++);
        String[] corrHeaders = {"Фактор зв'язку", "Коефіцієнт кореляції (r)", "Сила зв'язку", "Вербальна інтерпретація"};
        populateExcelHeaders(corrHeaderRow, corrHeaders, styles.headerStyle);

        CorrelationAnalysisDto correlations = analyzerService.calculateCorrelations();
        for (CorrelationResultDto corr : correlations.getAllCorrelations()) {
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, corr.getFactorName(), styles.textStyle);
            writeExcelCell(row, 1, corr.getCoefficient(), styles.decimalStyle);
            writeExcelCell(row, 2, corr.getStrength(), styles.textStyle);
            writeExcelCell(row, 3, corr.getInterpretation(), styles.textStyle);
        }

        sheet.createRow(rowIndex++);

        Row section3Row = sheet.createRow(rowIndex++);
        Cell s3Cell = section3Row.createCell(0);
        s3Cell.setCellValue("3. Рейтинг топ-каналів ЗВО за переглядами (FR-13)");
        s3Cell.setCellStyle(styles.sectionTitleStyle);

        Row chanHeaderRow = sheet.createRow(rowIndex++);
        String[] chanHeaders = {"Позиція", "Назва каналу YouTube", "Кількість відео", "Сумарні перегляди", "Середні перегляди"};
        populateExcelHeaders(chanHeaderRow, chanHeaders, styles.headerStyle);

        List<ChannelRatingDto> topChannels = analyzerService.getTopChannelsByViews(10);
        int chanRank = 1;
        for (ChannelRatingDto channel : topChannels) {
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, chanRank++, styles.integerStyle);
            writeExcelCell(row, 1, channel.getChannelName(), styles.textStyle);
            writeExcelCell(row, 2, channel.getVideoCount(), styles.integerStyle);
            writeExcelCell(row, 3, channel.getTotalViews(), styles.integerStyle);
            writeExcelCell(row, 4, channel.getAverageViews(), styles.decimalStyle);
        }

        sheet.createRow(rowIndex++);

        Row section4Row = sheet.createRow(rowIndex++);
        Cell s4Cell = section4Row.createCell(0);
        s4Cell.setCellValue("4. Рейтинг спеціальностей за популярністю (FR-13)");
        s4Cell.setCellStyle(styles.sectionTitleStyle);

        Row specHeaderRow = sheet.createRow(rowIndex++);
        String[] specHeaders = {"Позиція", "Спеціальність", "Кількість захистів", "Сумарні перегляди"};
        populateExcelHeaders(specHeaderRow, specHeaders, styles.headerStyle);

        List<SpecialtyRatingDto> topSpecialties = analyzerService.getTopSpecialtiesByPopularity(10);
        int specRank = 1;
        for (SpecialtyRatingDto specialty : topSpecialties) {
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, specRank++, styles.integerStyle);
            writeExcelCell(row, 1, specialty.getSpecialty(), styles.textStyle);
            writeExcelCell(row, 2, specialty.getDefenseCount(), styles.integerStyle);
            writeExcelCell(row, 3, specialty.getTotalViews(), styles.integerStyle);
        }

        autoSizeExcelColumns(sheet, 6);
    }

    private void buildExcelCouncilComparisonSheet(XSSFWorkbook workbook, ExcelStyles styles) {
        Sheet sheet = workbook.createSheet("Порівняння ЗВО");

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Порівняння подання інформації радами провідних ЗВО України (FR-14)");
        titleCell.setCellStyle(styles.sheetTitleStyle);

        sheet.createRow(rowIndex++);

        PresentationComparisonReport report = analyzerService.compareCouncils();

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = {
                "Заклад вищої освіти", "Відео проаналізовано", "ПІБ у назві (%)",
                "Тема у назві (%)", "Ступінь у назві (%)", "Маркер частини (%)",
                "Структурований опис (%)", "Шифр ради (%)", "Шифр спеціальності (%)",
                "Поділ на частини", "Характеристика стилю"
        };
        populateExcelHeaders(headerRow, headers, styles.headerStyle);

        for (UniversityPresentationProfile profile : report.getProfiles()) {
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, profile.getUniversityName(), styles.textStyle);
            writeExcelCell(row, 1, profile.getTotalVideosAnalyzed(), styles.integerStyle);
            writeExcelCell(row, 2, profile.getTitleHasAuthorPercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 3, profile.getTitleHasTopicPercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 4, profile.getTitleHasDegreePercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 5, profile.getTitleHasPartNumberPercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 6, profile.getDescriptionHasStructuredLabelsPercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 7, profile.getDescriptionHasCouncilCodePercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 8, profile.getDescriptionHasSpecialtyCodePercentage() / 100.0, styles.percentStyle);
            writeExcelCell(row, 9, profile.isMultiPartPracticeDetected() ? "Зафіксовано" : "Не виявлено", styles.textStyle);
            writeExcelCell(row, 10, profile.getStyleSummary(), styles.textStyle);
        }

        sheet.createRow(rowIndex++);

        Row cfHeaderRow = sheet.createRow(rowIndex++);
        Cell cfHeaderCell = cfHeaderRow.createCell(0);
        cfHeaderCell.setCellValue("Спільні риси публікацій наукових захистів:");
        cfHeaderCell.setCellStyle(styles.sectionTitleStyle);

        for (String feature : report.getCommonFeatures()) {
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, "- " + feature, styles.textStyle);
        }

        sheet.createRow(rowIndex++);

        Row diffHeaderRow = sheet.createRow(rowIndex++);
        Cell diffHeaderCell = diffHeaderRow.createCell(0);
        diffHeaderCell.setCellValue("Специфічні відмінності між радами ЗВО:");
        diffHeaderCell.setCellStyle(styles.sectionTitleStyle);

        for (String diff : report.getDifferences()) {
            Row row = sheet.createRow(rowIndex++);
            writeExcelCell(row, 0, "- " + diff, styles.textStyle);
        }

        sheet.createRow(rowIndex++);

        Row concHeaderRow = sheet.createRow(rowIndex++);
        Cell concHeaderCell = concHeaderRow.createCell(0);
        concHeaderCell.setCellValue("Аналітичний висновок та рекомендації:");
        concHeaderCell.setCellStyle(styles.sectionTitleStyle);

        Row concRow = sheet.createRow(rowIndex++);
        writeExcelCell(concRow, 0, report.getAnalyticalConclusion(), styles.textStyle);

        autoSizeExcelColumns(sheet, headers.length);
    }

    private PdfPTable createPdfKpiTable(List<DefenseAggregationDto> defenses,
                                        Font headerFont,
                                        Font cellFont,
                                        Font cellBoldFont) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(8f);

        int totalDefenses = defenses.size();
        int totalVideos = defenses.stream().mapToInt(DefenseAggregationDto::getTotalPartsCount).sum();
        long totalViews = defenses.stream().mapToLong(DefenseAggregationDto::getTotalViewCount).sum();
        List<Integer> views = defenses.stream().map(DefenseAggregationDto::getTotalViewCount).toList();
        double avgViews = analyzerService.calculateAverage(views);
        double medianViews = analyzerService.calculateMedian(views);

        String[] headers = {"Всього захистів", "Відеозаписів", "Сума переглядів", "Середні перегляди", "Медіана переглядів"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(23, 32, 51));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5f);
            table.addCell(cell);
        }

        table.addCell(createPdfKpiValueCell(String.valueOf(totalDefenses), cellBoldFont));
        table.addCell(createPdfKpiValueCell(String.valueOf(totalVideos), cellBoldFont));
        table.addCell(createPdfKpiValueCell(String.format("%,d", totalViews).replace(',', ' '), cellBoldFont));
        table.addCell(createPdfKpiValueCell(String.format("%.1f", avgViews), cellBoldFont));
        table.addCell(createPdfKpiValueCell(String.format("%.1f", medianViews), cellBoldFont));

        return table;
    }

    private PdfPCell createPdfKpiValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new Color(248, 249, 250));
        cell.setPadding(6f);
        cell.setBorderColor(new Color(221, 225, 231));
        return cell;
    }

    private PdfPTable createPdfDefensesTable(List<DefenseAggregationDto> defenses,
                                             MultiPartExportMode mode,
                                             Font headerFont,
                                             Font cellFont,
                                             Font cellBoldFont,
                                             Font cellSmallFont) {
        boolean isSummaryOnly = mode == MultiPartExportMode.SUMMARY_ONLY;
        boolean isPartsOnly = mode == MultiPartExportMode.PARTS_ONLY;

        if (isSummaryOnly) {
            PdfPTable table = new PdfPTable(new float[]{4f, 18f, 32f, 18f, 7f, 8f, 6f, 7f});
            table.setWidthPercentage(100f);

            String[] headers = {"№", "Здобувач (ПІБ)", "Тема дисертації", "Спеціальність / ЗВО", "Частин", "Перегляди", "Лайки", "Трив."};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(36, 49, 74));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            int index = 1;
            for (DefenseAggregationDto defense : defenses) {
                Color rowBg = (index % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;
                table.addCell(createPdfCell(String.valueOf(index++), cellFont, Element.ALIGN_CENTER, rowBg));
                table.addCell(createPdfCell(defense.getAuthorFullName(), cellBoldFont, Element.ALIGN_LEFT, rowBg));
                table.addCell(createPdfCell(defense.getTopic(), cellFont, Element.ALIGN_LEFT, rowBg));
                table.addCell(createPdfCell(formatSpecialtyAndUniversity(defense), cellSmallFont, Element.ALIGN_LEFT, rowBg));
                table.addCell(createPdfCell(String.valueOf(defense.getTotalPartsCount()), cellFont, Element.ALIGN_CENTER, rowBg));
                table.addCell(createPdfCell(String.format("%,d", defense.getTotalViewCount()).replace(',', ' '), cellBoldFont, Element.ALIGN_RIGHT, rowBg));
                table.addCell(createPdfCell(String.valueOf(defense.getTotalLikeCount()), cellFont, Element.ALIGN_RIGHT, rowBg));
                table.addCell(createPdfCell(defense.getFormattedTotalDuration(), cellSmallFont, Element.ALIGN_CENTER, rowBg));
            }
            return table;
        } else if (isPartsOnly) {
            PdfPTable table = new PdfPTable(new float[]{4f, 16f, 7f, 31f, 14f, 8f, 7f, 6f, 7f});
            table.setWidthPercentage(100f);

            String[] headers = {"№", "Здобувач", "Частина", "Назва відео", "Канал", "Дата", "Перегляди", "Лайки", "Частка"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(36, 49, 74));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            int index = 1;
            for (DefenseAggregationDto defense : defenses) {
                List<VideoPartDetailDto> parts = getDefensePartsOrFallback(defense);
                for (VideoPartDetailDto part : parts) {
                    Color rowBg = (index % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;
                    table.addCell(createPdfCell(String.valueOf(index++), cellFont, Element.ALIGN_CENTER, rowBg));
                    table.addCell(createPdfCell(defense.getAuthorFullName(), cellBoldFont, Element.ALIGN_LEFT, rowBg));
                    table.addCell(createPdfCell("Ч. " + part.getPartNumber(), cellFont, Element.ALIGN_CENTER, rowBg));
                    table.addCell(createPdfCell(part.getTitle(), cellSmallFont, Element.ALIGN_LEFT, rowBg));
                    table.addCell(createPdfCell(part.getChannelName(), cellSmallFont, Element.ALIGN_LEFT, rowBg));
                    table.addCell(createPdfCell(formatLocalDate(part.getPublishedAt()), cellSmallFont, Element.ALIGN_CENTER, rowBg));
                    table.addCell(createPdfCell(String.format("%,d", part.getViewCount()).replace(',', ' '), cellBoldFont, Element.ALIGN_RIGHT, rowBg));
                    table.addCell(createPdfCell(String.valueOf(part.getLikeCount()), cellFont, Element.ALIGN_RIGHT, rowBg));
                    table.addCell(createPdfCell(String.format("%.1f%%", defense.calculateViewShare(part)), cellSmallFont, Element.ALIGN_RIGHT, rowBg));
                }
            }
            return table;
        } else {
            PdfPTable table = new PdfPTable(new float[]{4f, 11f, 16f, 29f, 14f, 7f, 7f, 5f, 7f});
            table.setWidthPercentage(100f);

            String[] headers = {"№", "Рівень", "Здобувач (ПІБ)", "Тема / Назва відео", "Спеціальність / Канал", "Дата", "Перегляди", "Лайки", "Частка"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(36, 49, 74));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            int index = 1;
            for (DefenseAggregationDto defense : defenses) {
                Color summaryBg = new Color(237, 242, 247);
                table.addCell(createPdfCell(String.valueOf(index++), cellBoldFont, Element.ALIGN_CENTER, summaryBg));
                table.addCell(createPdfCell("Сумарно", cellBoldFont, Element.ALIGN_CENTER, summaryBg));
                table.addCell(createPdfCell(defense.getAuthorFullName(), cellBoldFont, Element.ALIGN_LEFT, summaryBg));
                table.addCell(createPdfCell(defense.getTopic(), cellBoldFont, Element.ALIGN_LEFT, summaryBg));
                table.addCell(createPdfCell(defense.getSpecialty(), cellSmallFont, Element.ALIGN_LEFT, summaryBg));
                table.addCell(createPdfCell(formatLocalDate(defense.getFirstPublishedAt()), cellSmallFont, Element.ALIGN_CENTER, summaryBg));
                table.addCell(createPdfCell(String.format("%,d", defense.getTotalViewCount()).replace(',', ' '), cellBoldFont, Element.ALIGN_RIGHT, summaryBg));
                table.addCell(createPdfCell(String.valueOf(defense.getTotalLikeCount()), cellBoldFont, Element.ALIGN_RIGHT, summaryBg));
                table.addCell(createPdfCell("100.0%", cellSmallFont, Element.ALIGN_RIGHT, summaryBg));

                List<VideoPartDetailDto> parts = getDefensePartsOrFallback(defense);
                for (VideoPartDetailDto part : parts) {
                    Color partBg = Color.WHITE;
                    table.addCell(createPdfCell("", cellFont, Element.ALIGN_CENTER, partBg));
                    table.addCell(createPdfCell("  Частина " + part.getPartNumber(), cellSmallFont, Element.ALIGN_LEFT, partBg));
                    table.addCell(createPdfCell("", cellFont, Element.ALIGN_LEFT, partBg));
                    table.addCell(createPdfCell(part.getTitle(), cellSmallFont, Element.ALIGN_LEFT, partBg));
                    table.addCell(createPdfCell(part.getChannelName(), cellSmallFont, Element.ALIGN_LEFT, partBg));
                    table.addCell(createPdfCell(formatLocalDate(part.getPublishedAt()), cellSmallFont, Element.ALIGN_CENTER, partBg));
                    table.addCell(createPdfCell(String.format("%,d", part.getViewCount()).replace(',', ' '), cellFont, Element.ALIGN_RIGHT, partBg));
                    table.addCell(createPdfCell(String.valueOf(part.getLikeCount()), cellFont, Element.ALIGN_RIGHT, partBg));
                    table.addCell(createPdfCell(String.format("%.1f%%", defense.calculateViewShare(part)), cellSmallFont, Element.ALIGN_RIGHT, partBg));
                }
            }
            return table;
        }
    }

    private PdfPTable createPdfDescriptiveStatisticsTable(Font headerFont, Font cellFont, Font cellBoldFont) {
        PdfPTable table = new PdfPTable(new float[]{25f, 15f, 15f, 15f, 15f, 15f});
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(8f);

        String[] headers = {"Показник", "Середнє", "Медіана", "Мінімум", "Максимум", "Сума"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(36, 49, 74));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }

        int index = 0;
        for (MetricRow item : getMetricRows()) {
            DescriptiveStatisticsDto stats = item.stats();
            Color rowBg = (index++ % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;
            table.addCell(createPdfCell(item.name(), cellBoldFont, Element.ALIGN_LEFT, rowBg));
            table.addCell(createPdfCell(String.format("%.2f", stats != null ? stats.getAverage() : 0.0), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%.2f", stats != null ? stats.getMedian() : 0.0), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%,d", stats != null ? (long) stats.getMin() : 0L).replace(',', ' '), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%,d", stats != null ? (long) stats.getMax() : 0L).replace(',', ' '), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%,d", stats != null ? (long) stats.getSum() : 0L).replace(',', ' '), cellBoldFont, Element.ALIGN_RIGHT, rowBg));
        }

        return table;
    }

    private record MetricRow(String name, DescriptiveStatisticsDto stats) {
    }

    private List<MetricRow> getMetricRows() {
        ua.edu.dissertation.dto.VideoMetricStatisticsDto overall = analyzerService.calculateOverallStatistics();
        List<MetricRow> list = new ArrayList<>();
        list.add(new MetricRow("Перегляди відео", overall != null ? overall.getViews() : null));
        list.add(new MetricRow("Вподобання (лайки)", overall != null ? overall.getLikes() : null));
        list.add(new MetricRow("Коментарі", overall != null ? overall.getComments() : null));
        return list;
    }

    private PdfPTable createPdfCorrelationTable(Font headerFont, Font cellFont, Font cellBoldFont) {
        PdfPTable table = new PdfPTable(new float[]{30f, 15f, 20f, 35f});
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(8f);

        String[] headers = {"Фактор зв'язку", "Коефіцієнт (r)", "Сила зв'язку", "Інтерпретація"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(36, 49, 74));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }

        CorrelationAnalysisDto correlations = analyzerService.calculateCorrelations();
        int index = 0;
        for (CorrelationResultDto corr : correlations.getAllCorrelations()) {
            Color rowBg = (index++ % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;
            table.addCell(createPdfCell(corr.getFactorName(), cellBoldFont, Element.ALIGN_LEFT, rowBg));
            table.addCell(createPdfCell(String.format("%.4f", corr.getCoefficient()), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(corr.getStrength(), cellFont, Element.ALIGN_CENTER, rowBg));
            table.addCell(createPdfCell(corr.getInterpretation(), cellFont, Element.ALIGN_LEFT, rowBg));
        }

        return table;
    }

    private PdfPTable createPdfCouncilComparisonTable(PresentationComparisonReport report,
                                                     Font headerFont,
                                                     Font cellFont,
                                                     Font cellBoldFont) {
        PdfPTable table = new PdfPTable(new float[]{22f, 8f, 10f, 10f, 10f, 10f, 10f, 20f});
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(8f);

        String[] headers = {"ЗВО", "Відео", "ПІБ (%)", "Тема (%)", "Ступінь (%)", "Маркер ч. (%)", "Шифр спец. (%)", "Стиль"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(36, 49, 74));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }

        int index = 0;
        for (UniversityPresentationProfile profile : report.getProfiles()) {
            Color rowBg = (index++ % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;
            table.addCell(createPdfCell(profile.getUniversityName(), cellBoldFont, Element.ALIGN_LEFT, rowBg));
            table.addCell(createPdfCell(String.valueOf(profile.getTotalVideosAnalyzed()), cellFont, Element.ALIGN_CENTER, rowBg));
            table.addCell(createPdfCell(String.format("%.0f%%", profile.getTitleHasAuthorPercentage()), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%.0f%%", profile.getTitleHasTopicPercentage()), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%.0f%%", profile.getTitleHasDegreePercentage()), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%.0f%%", profile.getTitleHasPartNumberPercentage()), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(String.format("%.0f%%", profile.getDescriptionHasSpecialtyCodePercentage()), cellFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createPdfCell(profile.getStyleSummary(), cellFont, Element.ALIGN_LEFT, rowBg));
        }

        return table;
    }

    private void buildPdfCouncilComparisonText(Document document,
                                               PresentationComparisonReport report,
                                               Font boldTextFont,
                                               Font textFont) throws Exception {
        Paragraph commonTitle = new Paragraph("Спільні риси публікацій вчених рад:", boldTextFont);
        commonTitle.setSpacingBefore(6f);
        commonTitle.setSpacingAfter(2f);
        document.add(commonTitle);

        for (String item : report.getCommonFeatures()) {
            Paragraph p = new Paragraph("• " + item, textFont);
            p.setIndentationLeft(12f);
            document.add(p);
        }

        Paragraph diffTitle = new Paragraph("Специфічні відмінності між радами ЗВО:", boldTextFont);
        diffTitle.setSpacingBefore(6f);
        diffTitle.setSpacingAfter(2f);
        document.add(diffTitle);

        for (String item : report.getDifferences()) {
            Paragraph p = new Paragraph("• " + item, textFont);
            p.setIndentationLeft(12f);
            document.add(p);
        }

        Paragraph conclusionTitle = new Paragraph("Аналітичний висновок та рекомендації щодо стандартизації:", boldTextFont);
        conclusionTitle.setSpacingBefore(6f);
        conclusionTitle.setSpacingAfter(2f);
        document.add(conclusionTitle);

        Paragraph conclusion = new Paragraph(report.getAnalyticalConclusion(), textFont);
        conclusion.setIndentationLeft(12f);
        document.add(conclusion);
    }

    private PdfPCell createPdfCell(String text, Font font, int alignment, Color background) {
        String safeText = text != null ? text : "";
        PdfPCell cell = new PdfPCell(new Phrase(safeText, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(background);
        cell.setPadding(4f);
        cell.setBorderColor(new Color(221, 225, 231));
        return cell;
    }

    private String formatSpecialtyAndUniversity(DefenseAggregationDto defense) {
        String specialty = defense.getSpecialty() != null ? defense.getSpecialty() : "";
        String uni = defense.getUniversityName() != null ? defense.getUniversityName() : "";
        boolean hasUni = !uni.isBlank();
        return hasUni ? specialty + " (" + uni + ")" : specialty;
    }

    private List<VideoPartDetailDto> getDefensePartsOrFallback(DefenseAggregationDto defense) {
        boolean hasParts = defense.getParts() != null && !defense.getParts().isEmpty();
        if (hasParts) {
            return defense.getParts();
        }
        VideoPartDetailDto syntheticPart = VideoPartDetailDto.builder()
                .videoId(defense.getDissertationId())
                .youtubeVideoId("")
                .partNumber(1)
                .title(defense.getTopic())
                .publishedAt(defense.getFirstPublishedAt())
                .durationSeconds(defense.getTotalDurationSeconds())
                .formattedDuration(defense.getFormattedTotalDuration())
                .viewCount(defense.getTotalViewCount())
                .likeCount(defense.getTotalLikeCount())
                .commentCount(defense.getTotalCommentCount())
                .channelName(defense.getChannelName())
                .build();
        return List.of(syntheticPart);
    }

    private String formatLocalDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private BaseFont getRegularFont() {
        if (cachedRegularFont == null) {
            cachedRegularFont = loadBaseFont(ROBOTO_REGULAR_RESOURCE, "Roboto-Regular.ttf");
        }
        return cachedRegularFont;
    }

    private BaseFont getBoldFont() {
        if (cachedBoldFont == null) {
            cachedBoldFont = loadBaseFont(ROBOTO_BOLD_RESOURCE, "Roboto-Bold.ttf");
        }
        return cachedBoldFont;
    }

    private BaseFont loadBaseFont(String resourcePath, String fontName) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            boolean isMissing = stream == null;
            if (isMissing) {
                throw new IllegalStateException("Файл шрифту не знайдено в ресурсах: " + resourcePath);
            }
            byte[] fontBytes = stream.readAllBytes();
            return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        } catch (Exception exception) {
            log.error("Помилка ініціалізації шрифту {}", resourcePath, exception);
            throw new IllegalStateException("Не вдалося завантажити TrueType шрифт: " + resourcePath, exception);
        }
    }

    private void populateExcelHeaders(Row row, String[] headers, CellStyle headerStyle) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeExcelCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void autoSizeExcelColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            int minWidth = 3000;
            int maxWidth = 16000;
            boolean isTooNarrow = currentWidth < minWidth;
            boolean isTooWide = currentWidth > maxWidth;
            if (isTooNarrow) {
                sheet.setColumnWidth(i, minWidth);
            } else if (isTooWide) {
                sheet.setColumnWidth(i, maxWidth);
            }
        }
    }

    private ExcelStyles createExcelStyles(XSSFWorkbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        XSSFFont regularFont = workbook.createFont();
        regularFont.setFontName("Calibri");
        regularFont.setFontHeightInPoints((short) 10);

        XSSFFont boldFont = workbook.createFont();
        boldFont.setFontName("Calibri");
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setBold(true);

        XSSFFont titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setBold(true);

        XSSFFont sectionFont = workbook.createFont();
        sectionFont.setFontName("Calibri");
        sectionFont.setFontHeightInPoints((short) 11);
        sectionFont.setBold(true);

        XSSFFont headerFont = workbook.createFont();
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFColor headerColor = new XSSFColor(new byte[]{(byte) 36, (byte) 49, (byte) 74}, null);
        XSSFColor summaryColor = new XSSFColor(new byte[]{(byte) 237, (byte) 242, (byte) 247}, null);
        XSSFColor subColor = new XSSFColor(new byte[]{(byte) 250, (byte) 250, (byte) 250}, null);

        XSSFCellStyle sheetTitleStyle = workbook.createCellStyle();
        sheetTitleStyle.setFont(titleFont);

        XSSFCellStyle sectionTitleStyle = workbook.createCellStyle();
        sectionTitleStyle.setFont(sectionFont);

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(headerStyle);

        XSSFCellStyle textStyle = workbook.createCellStyle();
        textStyle.setFont(regularFont);
        textStyle.setAlignment(HorizontalAlignment.LEFT);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(textStyle);

        XSSFCellStyle integerStyle = workbook.createCellStyle();
        integerStyle.setFont(regularFont);
        integerStyle.setDataFormat(dataFormat.getFormat("#,##0"));
        integerStyle.setAlignment(HorizontalAlignment.RIGHT);
        integerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(integerStyle);

        XSSFCellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setFont(regularFont);
        decimalStyle.setDataFormat(dataFormat.getFormat("0.00"));
        decimalStyle.setAlignment(HorizontalAlignment.RIGHT);
        decimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(decimalStyle);

        XSSFCellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setFont(regularFont);
        percentStyle.setDataFormat(dataFormat.getFormat("0.0%"));
        percentStyle.setAlignment(HorizontalAlignment.RIGHT);
        percentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(percentStyle);

        XSSFCellStyle summaryRowStyle = workbook.createCellStyle();
        summaryRowStyle.setFont(boldFont);
        summaryRowStyle.setFillForegroundColor(summaryColor);
        summaryRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        summaryRowStyle.setAlignment(HorizontalAlignment.LEFT);
        summaryRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(summaryRowStyle);

        XSSFCellStyle summaryIntegerStyle = workbook.createCellStyle();
        summaryIntegerStyle.setFont(boldFont);
        summaryIntegerStyle.setFillForegroundColor(summaryColor);
        summaryIntegerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        summaryIntegerStyle.setDataFormat(dataFormat.getFormat("#,##0"));
        summaryIntegerStyle.setAlignment(HorizontalAlignment.RIGHT);
        summaryIntegerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(summaryIntegerStyle);

        XSSFCellStyle summaryPercentStyle = workbook.createCellStyle();
        summaryPercentStyle.setFont(boldFont);
        summaryPercentStyle.setFillForegroundColor(summaryColor);
        summaryPercentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        summaryPercentStyle.setDataFormat(dataFormat.getFormat("0.0%"));
        summaryPercentStyle.setAlignment(HorizontalAlignment.RIGHT);
        summaryPercentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(summaryPercentStyle);

        XSSFCellStyle subRowStyle = workbook.createCellStyle();
        subRowStyle.setFont(regularFont);
        subRowStyle.setFillForegroundColor(subColor);
        subRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subRowStyle.setAlignment(HorizontalAlignment.LEFT);
        subRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(subRowStyle);

        return new ExcelStyles(
                sheetTitleStyle, sectionTitleStyle, headerStyle, textStyle,
                integerStyle, decimalStyle, percentStyle,
                summaryRowStyle, summaryIntegerStyle, summaryPercentStyle, subRowStyle
        );
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static class PdfFooterEvent extends PdfPageEventHelper {
        private final Font font;

        PdfFooterEvent(BaseFont baseFont) {
            this.font = new Font(baseFont, 7, Font.NORMAL, new Color(108, 117, 125));
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String footerText = "Система аналізу захистів дисертацій на YouTube  |  Сторінка " + writer.getPageNumber();
            Phrase phrase = new Phrase(footerText, font);
            Rectangle rect = document.getPageSize();
            float x = (rect.getLeft() + rect.getRight()) / 2f;
            float y = rect.getBottom() + 15f;
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, phrase, x, y, 0);
        }
    }

    private record ExcelStyles(
            CellStyle sheetTitleStyle,
            CellStyle sectionTitleStyle,
            CellStyle headerStyle,
            CellStyle textStyle,
            CellStyle integerStyle,
            CellStyle decimalStyle,
            CellStyle percentStyle,
            CellStyle summaryRowStyle,
            CellStyle summaryIntegerStyle,
            CellStyle summaryPercentStyle,
            CellStyle subRowStyle
    ) {
    }
}
