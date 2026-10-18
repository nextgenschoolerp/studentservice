package com.kvs.student.dto;

import com.kvs.student.enums.CertificateType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueBonafideCertificateRequest {

    @NotNull(message = "certificateType is required")
    private CertificateType certificateType;

    private String purpose;
}
