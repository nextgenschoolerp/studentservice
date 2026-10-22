package com.kvs.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Settings → Academic setup: the year's dates, whether it is the current year, and the school week. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAcademicSessionRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /** True makes this the current academic year (the others stop being current) */
    private Boolean isActive;

    @Pattern(regexp = "MON_FRI|MON_SAT", message = "Week structure must be MON_FRI or MON_SAT")
    private String weekStructure;
}
