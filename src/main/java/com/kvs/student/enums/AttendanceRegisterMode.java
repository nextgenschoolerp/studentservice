package com.kvs.student.enums;

/**
 * How many registers a school takes each day (Settings → Academic setup → Attendance registers).
 * DAILY: one register a day (stored as the AM session). AM_PM: a morning and an afternoon register; figures count half-day sessions.
 */
public enum AttendanceRegisterMode {
    DAILY,
    AM_PM;

    public int sessionsPerDay() { return this == AM_PM ? 2 : 1; }
}
