package com.kvs.student.dto;

import com.kvs.student.enums.CertificateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    private UUID certificateId;
    private UUID studentId;
    private String studentUid;
    private String studentName;
    private CertificateType certificateType;
    private String certificateNumber;
    private LocalDate issueDate;
    private String authorizedByUserId;
    private String secondSignOffUserId;
    private String qrVerificationToken;
    private String verificationUrl;
    private String remarks;
}
