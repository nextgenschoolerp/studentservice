package com.kvs.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPromotionRequest {

    @NotNull(message = "currentAcademicYearId is required")
    private UUID currentAcademicYearId;

    @NotNull(message = "nextAcademicYearId is required")
    private UUID nextAcademicYearId;

    @NotNull(message = "fromGradeId is required")
    private UUID fromGradeId;

    @NotNull(message = "toGradeId is required")
    private UUID toGradeId;

    private List<UUID> studentIds;
}
