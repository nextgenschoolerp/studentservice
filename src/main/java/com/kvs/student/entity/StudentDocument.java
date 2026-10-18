package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A file kept on a student's record (birth certificate, proof of address, medical care plan, …).
 */
@Entity
@Table(name = "student_documents", indexes = {
        @Index(name = "idx_student_doc_student", columnList = "student_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;
}
