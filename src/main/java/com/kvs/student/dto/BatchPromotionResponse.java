package com.kvs.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPromotionResponse {
    private UUID nextAcademicYearId;
    private UUID toGradeId;
    private Integer totalPromoted;
    private String status;
}
