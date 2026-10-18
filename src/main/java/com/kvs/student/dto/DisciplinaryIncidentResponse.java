package com.kvs.student.dto;

import com.kvs.student.enums.IncidentSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplinaryIncidentResponse {
    private UUID incidentId;
    private UUID studentId;
    private String studentUid;
    private LocalDate incidentDate;
    private IncidentSeverity severity;
    private String incidentCategory;
    private String description;
    private String actionTaken;
    private String counselingNotes;
    private String reportedByUserId;
}
