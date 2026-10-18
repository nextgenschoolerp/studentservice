package com.kvs.student.dto;

import com.kvs.student.enums.TransferRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSectionTransferRequest {

    @NotNull(message = "status is required")
    private TransferRequestStatus status;

    private String reviewRemarks;
}
