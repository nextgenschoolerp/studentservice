package com.kvs.student.dto;

import com.kvs.student.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.kvs.student.enums.AttendanceRegisterMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One student's attendance over a period (by default the active academic session to date): profile card and tab. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceSummaryResponse {
    private UUID studentId;
    private LocalDate from;
    private LocalDate to;
    /** Name of the period, e.g. the academic session "2026-2027" */
    private String periodLabel;
    /** DAILY: one mark per school day (the day register); AM_PM: half-day sessions */
    private AttendanceRegisterMode mode;
    /** Days (DAILY) or half-day sessions (AM_PM) in which the student was marked */
    private int sessionsMarked;
    private int present;
    private int late;
    private int excused;
    private int unexcused;
    /** (present + late) / sessionsMarked × 100, one decimal; null when nothing is marked */
    private BigDecimal attendanceRate;
    /** Today's marks; null when unmarked (DAILY: todayAm is the day's mark, todayPm is null) */
    private AttendanceStatus todayAm;
    private AttendanceStatus todayPm;
    /** Late arrivals and absences in the period, newest first */
    private List<AttendanceRecordResponse> absences;
}
