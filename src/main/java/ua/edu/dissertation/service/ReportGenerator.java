package ua.edu.dissertation.service;

import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.MultiPartExportMode;
import ua.edu.dissertation.dto.ReportFormat;

import java.util.List;

public interface ReportGenerator {

    byte[] createReport(ReportFormat format, List<DefenseAggregationDto> defenses, MultiPartExportMode mode);

    byte[] exportExcel(List<DefenseAggregationDto> defenses, MultiPartExportMode mode);

    byte[] exportPDF(List<DefenseAggregationDto> defenses, MultiPartExportMode mode);
}

