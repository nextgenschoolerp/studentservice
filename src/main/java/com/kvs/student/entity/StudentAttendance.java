package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.AttendanceSession;
import com.kvs.student.enums.AttendanceStatus;
import com.kvs.student.enums.FollowUpStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One student's mark in one register session (AM or PM) of one school day. */
@Entity
@Table(name = "student_attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_student_day_session", columnNames = {"student_id", "attendance_date", "attendance_session"})
}, indexes = {
        @Index(name = "idx_attendance_tenant_date", columnList = "tenant_id, attendance_date"),
        @Index(name = "idx_attendance_section_date", columnList = "section_id, attendance_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendance extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attendance_id")
    private UUID attendanceId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /** Class and section at the time of marking (a later section transfer keeps the history) */
    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_session", nullable = false, length = 2)
    private AttendanceSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "marked_by", nullable = false, length = 100)
    private String markedBy;

    /** Set for UNEXCUSED absences only */
    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_status", length = 20)
    private FollowUpStatus followUpStatus;

    @Column(name = "follow_up_note", length = 1000)
    private String followUpNote;

    @Column(name = "follow_up_by", length = 100)
    private String followUpBy;

    @Column(name = "follow_up_at")
    private Instant followUpAt;
}
