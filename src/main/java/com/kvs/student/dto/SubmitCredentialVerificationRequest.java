package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitCredentialVerificationRequest {

    @NotBlank(message = "studentUid is required")
    private String studentUid;

    @NotBlank(message = "requestorName is required")
    private String requestorName;

    @NotBlank(message = "requestorOrganization is required")
    private String requestorOrganization;

    @NotBlank(message = "credentialType is required")
    private String credentialType;

    private String remarks;
}
