package com.kvs.student.dto;

import com.kvs.student.enums.TransferRequestStatus;
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
public class SectionTransferResponse {
    private UUID transferId;
    private UUID studentId;
    private String studentUid;
    private String studentName;
    private UUID fromSectionId;
    private String fromSectionName;
    private UUID toSectionId;
    private String toSectionName;
    private String reason;
    private TransferRequestStatus status;
    private String requestedByUserId;
    private String approvedByUserId;
    private Instant approvedAt;
    private String reviewRemarks;
    private Instant createdAt;
}
