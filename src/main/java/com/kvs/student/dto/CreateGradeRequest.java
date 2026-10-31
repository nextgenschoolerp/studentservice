package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGradeRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "gradeName is required")
    @jakarta.validation.constraints.Size(max = 50, message = "gradeName cannot exceed 50 characters")
    private String gradeName;

    @NotBlank(message = "gradeCode is required")
    @jakarta.validation.constraints.Size(max = 20, message = "gradeCode cannot exceed 20 characters")
    private String gradeCode;

    @NotNull(message = "sequenceOrder is required")
    @jakarta.validation.constraints.Min(value = 1, message = "sequenceOrder must be at least 1")
    private Integer sequenceOrder;
}
