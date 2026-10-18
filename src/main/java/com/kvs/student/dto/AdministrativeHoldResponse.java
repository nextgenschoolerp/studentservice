package com.kvs.student.dto;

import com.kvs.student.enums.HoldType;
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
public class AdministrativeHoldResponse {
    private UUID holdId;
    private UUID studentId;
    private String studentUid;
    private String studentName;
    private HoldType holdType;
    private String reason;
    private Boolean isActive;
    private String placedByUserId;
    private Instant placedAt;
    private String releasedByUserId;
    private Instant releasedAt;
    private String releaseRemarks;
}
