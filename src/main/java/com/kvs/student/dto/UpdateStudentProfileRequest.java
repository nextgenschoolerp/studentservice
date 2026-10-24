package com.kvs.student.dto;

import com.kvs.student.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Personal and contact details of a student (PUT /students/{id}/profile). Blank optional fields clear the value. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentProfileRequest {

    @NotBlank(message = "firstName is required")
    private String firstName;

    @NotBlank(message = "lastName is required")
    private String lastName;

    private String preferredName;

    @NotNull(message = "dateOfBirth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "gender is required")
    private Gender gender;

    private String nationality;

    private LocalDate admissionDate;

    private String admissionNumber;

    private String residentialAddress;

    private String medicationInstructions;

    private String gpContact;

    private String collectionPermission;
}
