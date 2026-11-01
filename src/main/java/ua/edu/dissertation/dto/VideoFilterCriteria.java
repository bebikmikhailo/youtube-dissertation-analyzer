package ua.edu.dissertation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoFilterCriteria {

    @Size(max = 255, message = "Назва каналу не може перевищувати 255 символів")
    private String channelName;

    @Size(max = 255, message = "Назва ЗВО або ради не може перевищувати 255 символів")
    private String councilOrUniversity;

    @Size(max = 255, message = "Назва спеціальності не може перевищувати 255 символів")
    private String specialty;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedTo;

    @PositiveOrZero(message = "Мінімальна кількість переглядів не може бути від'ємною")
    private Integer minViews;

    @PositiveOrZero(message = "Максимальна кількість переглядів не може бути від'ємною")
    private Integer maxViews;

    @Size(max = 200, message = "Текст пошуку не може перевищувати 200 символів")
    private String searchText;

    @Builder.Default
    private SortField sortField = SortField.PUBLISHED_AT;

    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.DESC;

    @AssertTrue(message = "Початкова дата публікації не може бути пізнішою за кінцеву")
    public boolean isPublicationPeriodValid() {
        boolean hasBothDates = publishedFrom != null && publishedTo != null;
        return !hasBothDates || !publishedFrom.isAfter(publishedTo);
    }

    @AssertTrue(message = "Мінімальна кількість переглядів не може перевищувати максимальну")
    public boolean isViewRangeValid() {
        boolean hasBothLimits = minViews != null && maxViews != null;
        return !hasBothLimits || minViews <= maxViews;
    }

    @Getter
    public enum SortField {
        VIEWS("viewCount", "Переглядами"),
        LIKES("likeCount", "Вподобаннями"),
        COMMENTS("commentCount", "Коментарями"),
        PUBLISHED_AT("publishedAt", "Датою публікації"),
        DURATION("durationSeconds", "Тривалістю");

        private final String propertyName;
        private final String displayName;

        SortField(String propertyName, String displayName) {
            this.propertyName = propertyName;
            this.displayName = displayName;
        }
    }
}
