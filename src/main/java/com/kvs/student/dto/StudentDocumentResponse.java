package com.kvs.student.dto;

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
public class StudentDocumentResponse {
    private UUID documentId;
    private UUID studentId;
    private String documentType;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private String uploadedBy;
    private Instant uploadedAt;
    /** Relative API path that returns the file (needs the bearer token). */
    private String downloadUrl;
}
