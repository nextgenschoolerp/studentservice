package com.kvs.student.dto;

import com.kvs.student.enums.AttendanceRegisterMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** GET|PUT /api/v1/attendance/settings: the school's register mode; updatedAt is null while the default (DAILY) applies. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSettingsResponse {
    private String tenantId;
    private AttendanceRegisterMode mode;
    private int sessionsPerDay;
    private String updatedBy;
    private Instant updatedAt;
}
