package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PUT /api/v1/attendance/settings. A string, so an unknown value is answered with a readable 400. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAttendanceSettingsRequest {
    @NotBlank(message = "Choose DAILY (one register a day) or AM_PM (morning and afternoon registers)")
    private String mode;
}
