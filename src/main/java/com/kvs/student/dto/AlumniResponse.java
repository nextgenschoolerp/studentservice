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
public class AlumniResponse {
    private UUID alumniId;
    private UUID studentId;
    private String studentUid;
    private String fullName;
    private Integer graduationYear;
    private String finalGradePassed;
    private String alumniEmail;
    private String alumniPhone;
    private String higherEducationInstitution;
    private String currentOccupation;
    private LocalDate archivedDate;
}
