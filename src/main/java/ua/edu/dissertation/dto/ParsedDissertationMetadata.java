package ua.edu.dissertation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedDissertationMetadata {

    private String authorFullName;
    private String topic;
    private String specialtyCode;
    private String specialtyName;
    private String specialty;
    private String council;
    private String university;
}

