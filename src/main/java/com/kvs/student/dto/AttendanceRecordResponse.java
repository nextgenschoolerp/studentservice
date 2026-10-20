package com.kvs.student.dto;

import com.kvs.student.enums.AttendanceSession;
import com.kvs.student.enums.AttendanceStatus;
import com.kvs.student.enums.FollowUpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One attendance mark with the student and class: absence history and follow-up lists. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponse {
    private UUID attendanceId;
    private UUID studentId;
    private String studentUid;
    private String fullName;
    private String gradeName;
    private String sectionName;
    private LocalDate date;
    private AttendanceSession session;
    private AttendanceStatus status;
    private String reason;
    private String markedBy;
    private FollowUpStatus followUpStatus;
    private String followUpNote;
    private String followUpBy;
    private Instant followUpAt;
    /** For follow-ups: the student's emergency contact to call */
    private String emergencyContactPhone;
}
