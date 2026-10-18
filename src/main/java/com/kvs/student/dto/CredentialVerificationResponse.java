package com.kvs.student.dto;

import com.kvs.student.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialVerificationResponse {
    private UUID requestId;
    private String studentUid;
    private String studentName;
    private String requestorName;
    private String requestorOrganization;
    private String credentialType;
    private VerificationStatus status;
    private String verificationRemarks;
    private String verifiedByUserId;
    private Instant verifiedAt;
    private Instant createdAt;
}
