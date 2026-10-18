package com.kvs.student.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogActivityCreditRequest {

    private UUID houseId;

    @NotBlank(message = "activityName is required")
    private String activityName;

    @NotBlank(message = "activityCategory is required")
    private String activityCategory;

    @NotNull(message = "pointsAwarded is required")
    @Min(value = 1, message = "pointsAwarded must be at least 1")
    private Integer pointsAwarded;

    @NotNull(message = "eventDate is required")
    private LocalDate eventDate;

    private String remarks;
}
