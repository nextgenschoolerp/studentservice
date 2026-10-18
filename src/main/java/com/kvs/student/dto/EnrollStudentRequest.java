package com.kvs.student.dto;

import com.kvs.student.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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
public class EnrollStudentRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "branchId is required")
    private String branchId;

    @NotBlank(message = "firstName is required")
    private String firstName;

    @NotBlank(message = "lastName is required")
    private String lastName;

    @NotNull(message = "dateOfBirth is required")
    @Past(message = "dateOfBirth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "gender is required")
    private Gender gender;

    private String bloodGroup;
    private String medicalAlerts;
    private String emergencyContactPhone;

    private UUID gradeId;
    private UUID sectionId;
    private UUID houseId;
    private Integer rollNumber;

    private String guardianName;
    private String guardianRelationship;
    private String guardianPhone;
    private String guardianEmail;
}
