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
    private String gradeName;

    @NotBlank(message = "gradeCode is required")
    private String gradeCode;

    @NotNull(message = "sequenceOrder is required")
    private Integer sequenceOrder;
}
