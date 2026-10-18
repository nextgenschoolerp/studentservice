package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMedicalAlertsRequest {

    @NotBlank(message = "bloodGroup is required")
    private String bloodGroup;

    @NotBlank(message = "medicalAlerts details are required")
    private String medicalAlerts;

    @NotBlank(message = "emergencyContactPhone is required")
    private String emergencyContactPhone;
}
