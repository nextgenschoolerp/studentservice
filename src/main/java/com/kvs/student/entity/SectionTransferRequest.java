package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.TransferRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "section_transfer_requests", indexes = {
        @Index(name = "idx_str_student", columnList = "student_id"),
        @Index(name = "idx_str_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionTransferRequest extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "from_section_id", nullable = false)
    private UUID fromSectionId;

    @Column(name = "to_section_id", nullable = false)
    private UUID toSectionId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransferRequestStatus status = TransferRequestStatus.PENDING;

    @Column(name = "requested_by_user_id", length = 100)
    private String requestedByUserId;

    @Column(name = "approved_by_user_id", length = 100)
    private String approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "review_remarks", length = 500)
    private String reviewRemarks;
}
