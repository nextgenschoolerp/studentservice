package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.IncidentSeverity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "disciplinary_incidents", indexes = {
        @Index(name = "idx_incident_student", columnList = "student_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplinaryIncident extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private IncidentSeverity severity;

    @Column(name = "incident_category", nullable = false, length = 100)
    private String incidentCategory;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "action_taken", length = 500)
    private String actionTaken;

    @Column(name = "counseling_notes", length = 1000)
    private String counselingNotes;

    @Column(name = "reported_by_user_id", nullable = false, length = 100)
    private String reportedByUserId;
}
