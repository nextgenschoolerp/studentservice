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
public class IssueTransferCertificateRequest {

    @NotBlank(message = "reasonForLeaving is required")
    private String reasonForLeaving;

    @NotBlank(message = "conductRemarks is required")
    private String conductRemarks;

    @NotBlank(message = "secondSignOffUserId is required for dual administrative sign-off")
    private String secondSignOffUserId;
}
