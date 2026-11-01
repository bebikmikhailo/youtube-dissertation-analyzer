package ua.edu.dissertation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeSearchForm {

    @NotBlank(message = "Введіть пошуковий запит для YouTube")
    @Size(min = 3, max = 200, message = "Пошуковий запит повинен містити від 3 до 200 символів")
    private String query;
}

