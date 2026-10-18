package com.kvs.student.dto;

import com.kvs.student.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCredentialVerificationRequest {

    @NotNull(message = "status is required")
    private VerificationStatus status;

    private String verificationRemarks;
}
