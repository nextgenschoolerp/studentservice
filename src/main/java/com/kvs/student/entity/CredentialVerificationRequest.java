package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credential_verification_requests", indexes = {
        @Index(name = "idx_cred_student_uid", columnList = "student_uid"),
        @Index(name = "idx_cred_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialVerificationRequest extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "student_uid", nullable = false, length = 50)
    private String studentUid;

    @Column(name = "requestor_name", nullable = false, length = 100)
    private String requestorName;

    @Column(name = "requestor_organization", nullable = false, length = 150)
    private String requestorOrganization;

    @Column(name = "credential_type", nullable = false, length = 50)
    private String credentialType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "verification_remarks", length = 500)
    private String verificationRemarks;

    @Column(name = "verified_by_user_id", length = 100)
    private String verifiedByUserId;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
