package com.kvs.student.dto;

import com.kvs.student.enums.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDisciplinaryIncidentRequest {

    @NotNull(message = "incidentDate is required")
    @PastOrPresent(message = "incidentDate cannot be in the future")
    private LocalDate incidentDate;

    @NotNull(message = "severity is required")
    private IncidentSeverity severity;

    @NotBlank(message = "incidentCategory is required")
    private String incidentCategory;

    @NotBlank(message = "description is required")
    private String description;

    private String actionTaken;

    private String counselingNotes;
}
