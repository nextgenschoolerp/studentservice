package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.AttendanceSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A section's register for one session of one day: who submitted it and when (first submission and last change). */
@Entity
@Table(name = "attendance_registers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_register_section_day_session", columnNames = {"section_id", "register_date", "register_session"})
}, indexes = {
        @Index(name = "idx_register_tenant_date", columnList = "tenant_id, register_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRegister extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "register_id")
    private UUID registerId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "register_date", nullable = false)
    private LocalDate registerDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "register_session", nullable = false, length = 2)
    private AttendanceSession session;

    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "last_changed_by", length = 100)
    private String lastChangedBy;

    @Column(name = "last_changed_at")
    private Instant lastChangedAt;
}
