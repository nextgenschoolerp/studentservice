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
public class PhotoExportItemResponse {
    private UUID studentId;
    private String studentUid;
    private String fullName;
    private String gradeName;
    private String sectionName;
    private Integer rollNumber;
    private String photoUrl;
}
