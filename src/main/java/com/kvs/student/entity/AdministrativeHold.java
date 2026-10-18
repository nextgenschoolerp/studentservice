package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.HoldType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_administrative_holds", indexes = {
        @Index(name = "idx_hold_student", columnList = "student_id"),
        @Index(name = "idx_hold_active", columnList = "is_active")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministrativeHold extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hold_id")
    private UUID holdId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hold_type", nullable = false, length = 30)
    private HoldType holdType;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "placed_by_user_id", length = 100)
    private String placedByUserId;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "released_by_user_id", length = 100)
    private String releasedByUserId;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "release_remarks", length = 500)
    private String releaseRemarks;
}
