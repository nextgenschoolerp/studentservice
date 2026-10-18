package com.kvs.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRollListResponse {
    private UUID gradeId;
    private String gradeName;
    private UUID sectionId;
    private String sectionName;
    private LocalDate generatedDate;
    private Integer totalActiveStudents;
    private List<RollListStudentItem> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollListStudentItem {
        private UUID studentId;
        private String studentUid;
        private Integer rollNumber;
        private String fullName;
        private String gender;
        private String bloodGroup;
        private String houseName;
        private String emergencyContactPhone;
        private Boolean isOnHold;
    }
}
