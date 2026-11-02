package ua.edu.dissertation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ua.edu.dissertation.dto.ChannelRatingDto;
import ua.edu.dissertation.dto.CorrelationAnalysisDto;
import ua.edu.dissertation.dto.CorrelationResultDto;
import ua.edu.dissertation.dto.DashboardChartsDto;
import ua.edu.dissertation.dto.DashboardSummaryDto;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.DynamicsInterval;
import ua.edu.dissertation.dto.PresentationComparisonReport;
import ua.edu.dissertation.dto.SpecialtyRatingDto;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.ViewDynamicsPointDto;
import ua.edu.dissertation.dto.ViewDynamicsReportDto;
import ua.edu.dissertation.service.AnalyzerService;
import ua.edu.dissertation.service.DefenseCatalogService;

import java.util.List;

@Slf4j
@Controller
public class DashboardController {

    private static final int TOP_CHANNELS_LIMIT = 8;
    private static final int TOP_SPECIALTIES_LIMIT = 8;

    private final AnalyzerService analyzerService;
    private final DefenseCatalogService defenseCatalogService;
    private final ObjectMapper objectMapper;

    public DashboardController(AnalyzerService analyzerService,
                               DefenseCatalogService defenseCatalogService,
                               ObjectMapper objectMapper) {
        this.analyzerService = analyzerService;
        this.defenseCatalogService = defenseCatalogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(new VideoFilterCriteria());
        DashboardSummaryDto summary = buildSummary(defenses);

        List<ChannelRatingDto> topChannels = analyzerService.getTopChannelsByViews(TOP_CHANNELS_LIMIT);
        List<SpecialtyRatingDto> topSpecialties = analyzerService.getTopSpecialtiesByPopularity(TOP_SPECIALTIES_LIMIT);
        ViewDynamicsReportDto viewDynamics = analyzerService.buildViewDynamics(DynamicsInterval.DAYS);
        CorrelationAnalysisDto correlations = analyzerService.calculateCorrelations();
        PresentationComparisonReport councilComparison = analyzerService.compareCouncils();

        DashboardChartsDto chartsDto = buildChartsDto(topChannels, topSpecialties, viewDynamics, correlations);
        String chartsJson = serializeToJson(chartsDto);

        model.addAttribute("summary", summary);
        model.addAttribute("topChannels", topChannels);
        model.addAttribute("topSpecialties", topSpecialties);
        model.addAttribute("viewDynamics", viewDynamics);
        model.addAttribute("correlations", correlations);
        model.addAttribute("councilComparison", councilComparison);
        model.addAttribute("chartsJson", chartsJson);

        return "dashboard/index";
    }

    private DashboardSummaryDto buildSummary(List<DefenseAggregationDto> defenses) {
        boolean hasDefenses = defenses != null && !defenses.isEmpty();
        if (!hasDefenses) {
            return DashboardSummaryDto.builder()
                    .totalDefenses(0)
                    .totalViews(0L)
                    .averageViews(0.0)
                    .medianViews(0.0)
                    .multiPartCount(0L)
                    .totalVideosCount(0)
                    .build();
        }

        int totalDefenses = defenses.size();
        long totalViews = defenses.stream()
                .mapToLong(DefenseAggregationDto::getTotalViewCount)
                .sum();

        List<Integer> viewCounts = defenses.stream()
                .map(DefenseAggregationDto::getTotalViewCount)
                .toList();

        double averageViews = analyzerService.calculateAverage(viewCounts);
        double medianViews = analyzerService.calculateMedian(viewCounts);

        long multiPartCount = defenses.stream()
                .filter(DefenseAggregationDto::isMultiPart)
                .count();

        int totalVideosCount = defenses.stream()
                .mapToInt(DefenseAggregationDto::getTotalPartsCount)
                .sum();

        return DashboardSummaryDto.builder()
                .totalDefenses(totalDefenses)
                .totalViews(totalViews)
                .averageViews(averageViews)
                .medianViews(medianViews)
                .multiPartCount(multiPartCount)
                .totalVideosCount(totalVideosCount)
                .build();
    }

    private DashboardChartsDto buildChartsDto(List<ChannelRatingDto> channels,
                                              List<SpecialtyRatingDto> specialties,
                                              ViewDynamicsReportDto dynamics,
                                              CorrelationAnalysisDto correlations) {
        DashboardChartsDto dto = new DashboardChartsDto();

        boolean hasChannels = channels != null && !channels.isEmpty();
        if (hasChannels) {
            for (ChannelRatingDto ch : channels) {
                String label = ch.getChannelName() != null ? ch.getChannelName() : "Невідомий канал";
                dto.getChannelLabels().add(label);
                dto.getChannelViews().add(ch.getTotalViews());
                dto.getChannelVideoCounts().add(ch.getVideoCount());
            }
        }

        boolean hasSpecialties = specialties != null && !specialties.isEmpty();
        if (hasSpecialties) {
            for (SpecialtyRatingDto sp : specialties) {
                String label = sp.getSpecialty() != null ? sp.getSpecialty() : "Не вказано";
                dto.getSpecialtyLabels().add(label);
                dto.getSpecialtyCounts().add(sp.getDefenseCount());
                dto.getSpecialtyViews().add(sp.getTotalViews());
            }
        }

        boolean hasDynamicsPoints = dynamics != null && dynamics.getPoints() != null && !dynamics.getPoints().isEmpty();
        if (hasDynamicsPoints) {
            for (ViewDynamicsPointDto point : dynamics.getPoints()) {
                dto.getDynamicsLabels().add(point.getPeriod());
                dto.getDynamicsViews().add(point.getTotalViews());
                dto.getDynamicsGrowths().add(point.getViewsGrowth());
            }
        }

        boolean hasCorrelations = correlations != null && correlations.getAllCorrelations() != null;
        if (hasCorrelations) {
            for (CorrelationResultDto corr : correlations.getAllCorrelations()) {
                dto.getCorrelationLabels().add(corr.getFactorName());
                dto.getCorrelationCoefficients().add(corr.getCoefficient());
                dto.getCorrelationStrengths().add(corr.getStrength());
            }
        }

        return dto;
    }

    private String serializeToJson(DashboardChartsDto chartsDto) {
        try {
            return objectMapper.writeValueAsString(chartsDto);
        } catch (JsonProcessingException e) {
            log.error("Помилка серіалізації структури графіків дашборду у JSON", e);
            return "{}";
        }
    }
}
