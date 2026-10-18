package com.kvs.student.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordHealthMetricRequest {

    @NotNull(message = "checkupDate is required")
    private LocalDate checkupDate;

    @NotNull(message = "heightCm is required")
    @DecimalMin(value = "30.0", message = "heightCm must be at least 30 cm")
    private BigDecimal heightCm;

    @NotNull(message = "weightKg is required")
    @DecimalMin(value = "2.0", message = "weightKg must be at least 2 kg")
    private BigDecimal weightKg;

    private String visionLeft;
    private String visionRight;
    private String healthNotes;
}
