package com.kvs.student.dto;

import com.kvs.student.enums.Gender;
import com.kvs.student.enums.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private UUID studentId;
    private String tenantId;
    private String branchId;
    private String studentUid;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bloodGroup;
    private String medicalAlerts;
    private String emergencyContactPhone;
    private UUID gradeId;
    private String gradeName;
    private UUID sectionId;
    private String sectionName;
    private UUID houseId;
    private String houseName;
    private Integer rollNumber;
    private StudentStatus studentStatus;
    private String parentAccountId;
    private String idCardBarcode;
    private String idCardQrCode;
    private Instant createdAt;
    private Instant updatedAt;
}
