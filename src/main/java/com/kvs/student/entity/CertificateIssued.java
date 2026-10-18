package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.CertificateType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificates_issued", indexes = {
        @Index(name = "idx_cert_student", columnList = "student_id"),
        @Index(name = "idx_cert_number", columnList = "certificate_number", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateIssued extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "certificate_id")
    private UUID certificateId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 50)
    private CertificateType certificateType;

    @Column(name = "certificate_number", unique = true, nullable = false, length = 50)
    private String certificateNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "authorized_by_user_id", nullable = false, length = 100)
    private String authorizedByUserId;

    @Column(name = "second_sign_off_user_id", length = 100)
    private String secondSignOffUserId;

    @Column(name = "qr_verification_token", length = 255)
    private String qrVerificationToken;

    @Column(name = "pdf_vault_path", length = 255)
    private String pdfVaultPath;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
