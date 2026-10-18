package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveAlumniRequest {

    @NotNull(message = "studentId is required")
    private UUID studentId;

    @NotNull(message = "graduationYear is required")
    private Integer graduationYear;

    @NotBlank(message = "finalGradePassed is required")
    private String finalGradePassed;

    private String alumniEmail;
    private String alumniPhone;
    private String higherEducationInstitution;
    private String currentOccupation;
}
