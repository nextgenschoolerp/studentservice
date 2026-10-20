package com.kvs.student.enums;

/** A student's mark in one register session. Unmarked students have no record. */
public enum AttendanceStatus {
    PRESENT,
    LATE,
    /** Absent with a confirmed reason (reason required) */
    EXCUSED,
    /** Absent without a confirmed reason: opens a guardian follow-up */
    UNEXCUSED
}
