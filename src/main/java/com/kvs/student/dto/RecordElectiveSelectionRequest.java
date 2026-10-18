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
public class RecordElectiveSelectionRequest {

    @NotNull(message = "academicYearId is required")
    private UUID academicYearId;

    @NotBlank(message = "firstLanguage is required")
    private String firstLanguage;

    @NotBlank(message = "secondLanguage is required")
    private String secondLanguage;

    private String thirdLanguage;

    @NotBlank(message = "electiveSubjectCode is required")
    private String electiveSubjectCode;
}
