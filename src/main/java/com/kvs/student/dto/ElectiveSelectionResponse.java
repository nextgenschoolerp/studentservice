package com.kvs.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectiveSelectionResponse {
    private UUID selectionId;
    private UUID studentId;
    private String studentUid;
    private String studentName;
    private UUID academicYearId;
    private String firstLanguage;
    private String secondLanguage;
    private String thirdLanguage;
    private String electiveSubjectCode;
    private String status;
    private String approvedByUserId;
}
