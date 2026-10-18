package com.kvs.student.dto;

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
public class AssignSectionHouseRequest {

    @NotNull(message = "gradeId is required")
    private UUID gradeId;

    @NotNull(message = "sectionId is required")
    private UUID sectionId;

    private UUID houseId;

    private Integer rollNumber;
}
