package com.kvs.student.dto;

import com.kvs.student.enums.AttendanceStatus;
import com.kvs.student.enums.FollowUpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A section's register for one day: both sessions, one row per active student in roll-number order. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRegisterResponse {
    private UUID gradeId;
    private String gradeName;
    private UUID sectionId;
    private String sectionName;
    private LocalDate date;
    private SessionInfo am;
    private SessionInfo pm;
    private List<StudentRow> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private boolean submitted;
        private String submittedBy;
        private Instant submittedAt;
        private String lastChangedBy;
        private Instant lastChangedAt;
        private int present;
        private int late;
        private int excused;
        private int unexcused;
        private int unmarked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRow {
        private UUID studentId;
        private String studentUid;
        private Integer rollNumber;
        private String fullName;
        private Mark am;
        private Mark pm;
    }

    /** null when the student is unmarked in that session */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mark {
        private UUID attendanceId;
        private AttendanceStatus status;
        private String reason;
        private FollowUpStatus followUpStatus;
    }
}
