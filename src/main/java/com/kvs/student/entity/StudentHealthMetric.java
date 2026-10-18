package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_health_metrics", indexes = {
        @Index(name = "idx_health_student", columnList = "student_id"),
        @Index(name = "idx_health_checkup_date", columnList = "checkup_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentHealthMetric extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_id")
    private UUID metricId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "checkup_date", nullable = false)
    private LocalDate checkupDate;

    @Column(name = "height_cm", precision = 5, scale = 2, nullable = false)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2, nullable = false)
    private BigDecimal weightKg;

    @Column(name = "bmi", precision = 4, scale = 2)
    private BigDecimal bmi;

    @Column(name = "vision_left", length = 20)
    private String visionLeft;

    @Column(name = "vision_right", length = 20)
    private String visionRight;

    @Column(name = "health_notes", length = 500)
    private String healthNotes;

    @Column(name = "checked_by_user_id", length = 100)
    private String checkedByUserId;
}
