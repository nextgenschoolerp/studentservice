package com.kvs.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * School-wide attendance of one day, from the morning (AM) registers: the day's headline figure.
 * Also used for each day of the trend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private LocalDate date;
    /** Active students on roll */
    private int totalStudents;
    /** PRESENT + LATE */
    private int present;
    private int late;
    private int excused;
    private int unexcused;
    private int unmarked;
    /** present / students marked in the AM registers × 100, one decimal; null until a register is in (registers still open don't pull it down) */
    private BigDecimal presentRate;
    /** Sections with active students whose AM / PM register has been submitted */
    private int registersExpected;
    private int amRegistersSubmitted;
    private int pmRegistersSubmitted;
    /** UNEXCUSED absences of the day still awaiting guardian follow-up */
    private int openFollowUps;
}
