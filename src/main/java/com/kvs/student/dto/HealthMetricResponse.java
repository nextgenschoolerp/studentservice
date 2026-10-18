package com.kvs.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricResponse {
    private UUID metricId;
    private UUID studentId;
    private String studentUid;
    private String studentName;
    private LocalDate checkupDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal bmi;
    private String visionLeft;
    private String visionRight;
    private String healthNotes;
    private String checkedByUserId;
}
