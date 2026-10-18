package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.StudentDocumentResponse;
import com.kvs.student.dto.StudentResponse;
import com.kvs.student.entity.StudentDocument;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.repo.StudentDocumentRepository;
import com.kvs.student.repo.StudentMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Student photo and document files (Edit student → Change photo / Upload documents).
 * Files are kept in the {@link StudentFileStorage} vault; the record keeps the metadata.
 */
@Service
@Slf4j
public class StudentFileService {

    public static final Set<String> DOCUMENT_TYPES = Set.of("BIRTH_CERTIFICATE", "PROOF_OF_ADDRESS", "TRANSFER_CERTIFICATE",
            "REPORT_CARD", "MEDICAL_CARE_PLAN", "IDENTITY_DOCUMENT", "OTHER");
    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
    /** Value of profilePhotoUrl for a photo stored here (relative to the service; needs the bearer token). */
    static final String PHOTO_PREFIX = "vault:";

    @Autowired
    private StudentMasterRepository studentRepository;
    @Autowired
    private StudentDocumentRepository documentRepository;
    @Autowired
    private StudentFileStorage storage;
    @Autowired
    private StudentService studentService;
    @Autowired
    private TenantAccessGuard tenantGuard;
    @Autowired
    private StudentAuditService auditService;

    @Transactional
    public StudentResponse uploadPhoto(UUID studentId, MultipartFile file) {
        StudentMaster student = loadStudent(studentId);
        StudentFileStorage.StoredFile stored = storage.store(student.getTenantId(), studentId, file, StudentFileStorage.IMAGE_TYPES, MAX_PHOTO_BYTES);
        String previous = student.getProfilePhotoUrl();
        student.setProfilePhotoUrl(PHOTO_PREFIX + stored.relativePath());
        studentRepository.save(student);
        if (previous != null && previous.startsWith(PHOTO_PREFIX)) {
            storage.delete(previous.substring(PHOTO_PREFIX.length()));
        }
        auditService.logSuccess("student:" + studentId, "UPLOAD_PHOTO",
                "Uploaded profile photo for " + student.getStudentUid() + " (" + file.getSize() + " bytes, SHA-256 " + stored.checksumSha256() + ")");
        return studentService.getStudent(studentId);
    }

    @Transactional(readOnly = true)
    public StoredContent readPhoto(UUID studentId) {
        StudentMaster student = loadStudent(studentId);
        String url = student.getProfilePhotoUrl();
        byte[] bytes = url != null && url.startsWith(PHOTO_PREFIX) ? storage.read(url.substring(PHOTO_PREFIX.length())) : null;
        if (bytes == null) {
            throw new ResourceNotFoundException("StudentPhoto", "studentId", studentId);
        }
        return new StoredContent(bytes, url.endsWith(".png") ? "image/png" : "image/jpeg", student.getStudentUid() + "-photo");
    }

    @Transactional
    public StudentDocumentResponse uploadDocument(UUID studentId, String documentType, MultipartFile file) {
        if (documentType == null || !DOCUMENT_TYPES.contains(documentType)) {
            throw new BadRequestException("documentType must be one of " + DOCUMENT_TYPES);
        }
        StudentMaster student = loadStudent(studentId);
        StudentFileStorage.StoredFile stored = storage.store(student.getTenantId(), studentId, file, StudentFileStorage.DOCUMENT_TYPES, MAX_DOCUMENT_BYTES);
        String name = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
        StudentDocument saved = documentRepository.save(StudentDocument.builder()
                .studentId(studentId)
                .tenantId(student.getTenantId())
                .documentType(documentType)
                .originalFileName(name.length() > 255 ? name.substring(0, 255) : name)
                .filePath(stored.relativePath())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .checksumSha256(stored.checksumSha256())
                .uploadedBy(SecurityUtils.getCurrentUserEmail().orElse("staff"))
                .build());
        auditService.logSuccess("student:" + studentId, "UPLOAD_STUDENT_DOCUMENT",
                "Uploaded " + documentType + " for " + student.getStudentUid() + " | Document: " + saved.getDocumentId());
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> listDocuments(UUID studentId) {
        loadStudent(studentId);
        return documentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public StoredContent readDocument(UUID documentId) {
        StudentDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentDocument", "documentId", documentId));
        tenantGuard.requireTenantRecord(doc.getTenantId(), "StudentDocument", "documentId", documentId);
        byte[] bytes = storage.read(doc.getFilePath());
        if (bytes == null) {
            throw new ResourceNotFoundException("StudentDocumentFile", "documentId", documentId);
        }
        return new StoredContent(bytes, doc.getContentType(), doc.getOriginalFileName());
    }

    private StudentMaster loadStudent(UUID studentId) {
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));
        tenantGuard.requireTenantRecord(student.getTenantId(), "StudentMaster", "studentId", studentId);
        return student;
    }

    private StudentDocumentResponse map(StudentDocument d) {
        return StudentDocumentResponse.builder()
                .documentId(d.getDocumentId())
                .studentId(d.getStudentId())
                .documentType(d.getDocumentType())
                .originalFileName(d.getOriginalFileName())
                .contentType(d.getContentType())
                .fileSizeBytes(d.getFileSizeBytes())
                .uploadedBy(d.getUploadedBy())
                .uploadedAt(d.getCreatedAt())
                .downloadUrl("/api/v1/students/documents/" + d.getDocumentId() + "/file")
                .build();
    }

    public record StoredContent(byte[] bytes, String contentType, String fileName) {
    }
}
