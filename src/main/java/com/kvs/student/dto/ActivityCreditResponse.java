package com.kvs.student.dto;

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
public class ActivityCreditResponse {
    private UUID creditId;
    private UUID studentId;
    private String studentUid;
    private String studentName;
    private UUID houseId;
    private String houseName;
    private String activityName;
    private String activityCategory;
    private Integer pointsAwarded;
    private LocalDate eventDate;
    private String remarks;
    private String awardedByUserId;
}
