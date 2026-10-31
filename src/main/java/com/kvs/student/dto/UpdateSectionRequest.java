package com.kvs.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Changes a section's seat capacity (Settings → Academic setup → Classes, sections & houses). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSectionRequest {

    @NotNull(message = "maxCapacity is required")
    @Min(value = 1, message = "maxCapacity must be at least 1")
    @Max(value = 100, message = "maxCapacity cannot exceed 100")
    private Integer maxCapacity;
}
