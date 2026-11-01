package ua.edu.dissertation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.dissertation.client.youtube.exception.QuotaExceededException;
import ua.edu.dissertation.client.youtube.exception.YouTubeApiException;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.dto.CollectionResultDto;
import ua.edu.dissertation.dto.DefenseAggregationDto;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.YouTubeSearchForm;
import ua.edu.dissertation.service.DataCollectorService;
import ua.edu.dissertation.service.DefenseCatalogService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Controller
public class DefenseController {

    static final String FLASH_SUCCESS = "flashSuccess";
    static final String FLASH_WARNING = "flashWarning";
    static final String FLASH_ERROR = "flashError";

    private static final String REDIRECT_TO_CATALOG = "redirect:/defenses";
    private static final Locale UKRAINIAN = Locale.forLanguageTag("uk");

    private final DefenseCatalogService defenseCatalogService;
    private final DataCollectorService dataCollectorService;
    private final MessageSource messageSource;

    public DefenseController(DefenseCatalogService defenseCatalogService,
                             DataCollectorService dataCollectorService,
                             MessageSource messageSource) {
        this.defenseCatalogService = defenseCatalogService;
        this.dataCollectorService = dataCollectorService;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public String redirectToCatalog() {
        return REDIRECT_TO_CATALOG;
    }

    @GetMapping("/defenses")
    public String listDefenses(@Valid @ModelAttribute("filter") VideoFilterCriteria filter,
                               BindingResult bindingResult,
                               Model model) {
        VideoFilterCriteria appliedCriteria = bindingResult.hasErrors() ? new VideoFilterCriteria() : filter;
        if (bindingResult.hasErrors()) {
            model.addAttribute("filterErrors", collectMessages(bindingResult));
        }

        List<DefenseAggregationDto> defenses = defenseCatalogService.findDefenses(appliedCriteria);
        int videoCount = defenses.stream().mapToInt(DefenseAggregationDto::getTotalPartsCount).sum();

        model.addAttribute("defenses", defenses);
        model.addAttribute("videoCount", videoCount);
        model.addAttribute("channelNames", defenseCatalogService.findChannelNames());
        model.addAttribute("universities", defenseCatalogService.findUniversities());
        model.addAttribute("specialties", defenseCatalogService.findSpecialties());
        model.addAttribute("sortFields", VideoFilterCriteria.SortField.values());
        if (!model.containsAttribute("searchForm")) {
            model.addAttribute("searchForm", new YouTubeSearchForm());
        }
        return "defenses/list";
    }

    @GetMapping("/defenses/{id}")
    public String showDefense(@PathVariable("id") @Positive Long id,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Optional<DefenseAggregationDto> defense = defenseCatalogService.findDefense(id);
        if (defense.isEmpty()) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Захист дисертації з ідентифікатором " + id + " не знайдено");
            return REDIRECT_TO_CATALOG;
        }
        model.addAttribute("defense", defense.get());
        return "defenses/detail";
    }

    @PostMapping("/defenses/collect")
    public String collectDefenses(@Valid @ModelAttribute("searchForm") YouTubeSearchForm searchForm,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, String.join(". ", collectMessages(bindingResult)));
            redirectAttributes.addFlashAttribute("searchForm", searchForm);
            return REDIRECT_TO_CATALOG;
        }

        String query = searchForm.getQuery().trim();
        try {
            CollectionResultDto result = defenseCatalogService.collectDefenses(query);
            if (result.isEmpty()) {
                redirectAttributes.addFlashAttribute(FLASH_WARNING, "За запитом «" + query + "» відео не знайдено");
            } else {
                redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "За запитом «" + query + "» збережено відео: "
                        + result.collectedVideoCount() + ", сформовано захистів: " + result.mergedDefenseCount());
            }
        } catch (QuotaExceededException exception) {
            log.warn("Пошук за запитом '{}' зупинено через вичерпання квоти", query);
            redirectAttributes.addFlashAttribute(FLASH_WARNING, exception.getMessage());
        } catch (YouTubeApiException exception) {
            log.error("Помилка пошуку відео за запитом '{}': {}", query, exception.getMessage());
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Не вдалося виконати пошук на YouTube. " + exception.getMessage());
        }
        return REDIRECT_TO_CATALOG;
    }

    @PostMapping("/defenses/refresh-statistics")
    public String refreshStatistics(RedirectAttributes redirectAttributes) {
        try {
            List<DefenseVideo> updatedVideos = dataCollectorService.updateStatistics();
            if (updatedVideos.isEmpty()) {
                redirectAttributes.addFlashAttribute(FLASH_WARNING, "Немає збережених відео для оновлення статистики");
            } else {
                redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "Статистику оновлено для відео: " + updatedVideos.size());
            }
        } catch (QuotaExceededException exception) {
            log.warn("Оновлення статистики зупинено через вичерпання квоти");
            redirectAttributes.addFlashAttribute(FLASH_WARNING, exception.getMessage());
        } catch (YouTubeApiException exception) {
            log.error("Помилка оновлення статистики: {}", exception.getMessage());
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Не вдалося оновити статистику. " + exception.getMessage());
        }
        return REDIRECT_TO_CATALOG;
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    public String handleInvalidDefenseId(HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request)
                .put(FLASH_ERROR, "Некоректний ідентифікатор захисту дисертації");
        return REDIRECT_TO_CATALOG;
    }

    private List<String> collectMessages(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> messageSource.getMessage(error, UKRAINIAN))
                .distinct()
                .toList();
    }
}
