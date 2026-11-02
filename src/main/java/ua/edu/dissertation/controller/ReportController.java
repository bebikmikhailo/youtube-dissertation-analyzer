package ua.edu.dissertation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.MultiPartExportMode;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.service.DefenseCatalogService;
import ua.edu.dissertation.service.ReportGeneratorService;

import java.util.List;

@Slf4j
@Controller
public class ReportController {

    private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportGeneratorService reportGeneratorService;
    private final DefenseCatalogService defenseCatalogService;

    public ReportController(ReportGeneratorService reportGeneratorService,
                            DefenseCatalogService defenseCatalogService) {
        this.reportGeneratorService = reportGeneratorService;
        this.defenseCatalogService = defenseCatalogService;
    }

    @GetMapping({"/reports/excel", "/defenses/export/excel"})
    public ResponseEntity<byte[]> exportExcel(@RequestParam(name = "mode", defaultValue = "SUMMARY_ONLY") String mode,
                                              @ModelAttribute VideoFilterCriteria filter) {
        MultiPartExportMode exportMode = MultiPartExportMode.fromValue(mode);
        VideoFilterCriteria effectiveFilter = filter != null ? filter : new VideoFilterCriteria();
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(effectiveFilter);

        log.info("Формування звіту Excel у режимі '{}', захистів: {}", exportMode.name(), defenses.size());
        byte[] excelContent = reportGeneratorService.exportExcel(defenses, exportMode);

        String filename = "dissertations_report_" + exportMode.name().toLowerCase() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelContent);
    }

    @GetMapping({"/reports/pdf", "/defenses/export/pdf"})
    public ResponseEntity<byte[]> exportPdf(@RequestParam(name = "mode", defaultValue = "SUMMARY_ONLY") String mode,
                                            @ModelAttribute VideoFilterCriteria filter) {
        MultiPartExportMode exportMode = MultiPartExportMode.fromValue(mode);
        VideoFilterCriteria effectiveFilter = filter != null ? filter : new VideoFilterCriteria();
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(effectiveFilter);

        log.info("Формування звіту PDF у режимі '{}', захистів: {}", exportMode.name(), defenses.size());
        byte[] pdfContent = reportGeneratorService.exportPDF(defenses, exportMode);

        String filename = "dissertations_report_" + exportMode.name().toLowerCase() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfContent);
    }
}

