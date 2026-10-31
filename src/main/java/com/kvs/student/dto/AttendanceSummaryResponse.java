package com.kvs.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.kvs.student.enums.AttendanceRegisterMode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * School-wide attendance of one day: the day's headline figure, also used for each day of the trend.
 * With one register a day (DAILY) the figures are students; with morning and afternoon registers (AM_PM) they are half-day
 * sessions, so a student counts twice a day and {@code possible} = students × 2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private LocalDate date;
    /** DAILY or AM_PM, the school's register mode the figures follow */
    private AttendanceRegisterMode mode;
    /** 1 (DAILY) or 2 (AM_PM) */
    private int sessionsPerDay;
    /** Active students on roll */
    private int totalStudents;
    /** Marks possible today: totalStudents × sessionsPerDay */
    private int possible;
    /** PRESENT + LATE */
    private int present;
    private int late;
    private int excused;
    private int unexcused;
    private int unmarked;
    /** present / marks taken × 100, one decimal; null until a register is in (registers still open don't pull it down) */
    private BigDecimal presentRate;
    /** Registers due: sections with active students × sessionsPerDay */
    private int registersExpected;
    /** Registers submitted that count in this mode (DAILY: the day registers; AM_PM: morning + afternoon) */
    private int registersSubmitted;
    /** Per session, whatever the mode (DAILY registers are stored as AM) */
    private int amRegistersSubmitted;
    private int pmRegistersSubmitted;
    /** UNEXCUSED absences of the day still awaiting guardian follow-up */
    private int openFollowUps;
}
