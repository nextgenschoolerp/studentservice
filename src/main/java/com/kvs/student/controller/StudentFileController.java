package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.StudentDocumentResponse;
import com.kvs.student.dto.StudentResponse;
import com.kvs.student.service.StudentFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Student photo and document files: Edit student → Change photo / Upload documents, and the profile's Documents tab.
 * (US-03014's POST /{studentId}/photo stores a photo URL; these endpoints take the file itself.)
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/students")
public class StudentFileController {

    private static final String VIEW_ROLES = "hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'CLASS_TEACHER', 'SCHOOL_PRINCIPAL', 'SCHOOL_NURSE')";

    @Autowired
    private StudentFileService fileService;

    @PostMapping(value = "/{studentId}/photo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('REGISTRAR', 'OFFICE_CLERK', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> uploadPhoto(@PathVariable("studentId") UUID studentId,
                                                                    @RequestPart("file") MultipartFile file) {
        log.info("Uploading profile photo for student {}", studentId);
        return ResponseEntity.ok(ApiResponse.success("Student photo uploaded", fileService.uploadPhoto(studentId, file)));
    }

    @GetMapping("/{studentId}/photo/file")
    @PreAuthorize(VIEW_ROLES)
    public ResponseEntity<byte[]> photo(@PathVariable("studentId") UUID studentId) {
        return file(fileService.readPhoto(studentId), false);
    }

    @PostMapping(value = "/{studentId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('REGISTRAR', 'OFFICE_CLERK', 'SCHOOL_NURSE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentDocumentResponse>> uploadDocument(@PathVariable("studentId") UUID studentId,
                                                                               @RequestParam("documentType") String documentType,
                                                                               @RequestPart("file") MultipartFile file) {
        log.info("Uploading {} for student {}", documentType, studentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded", fileService.uploadDocument(studentId, documentType, file)));
    }

    @GetMapping("/{studentId}/documents")
    @PreAuthorize(VIEW_ROLES)
    public ResponseEntity<ApiResponse<List<StudentDocumentResponse>>> listDocuments(@PathVariable("studentId") UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(fileService.listDocuments(studentId)));
    }

    @GetMapping("/documents/{documentId}/file")
    @PreAuthorize(VIEW_ROLES)
    public ResponseEntity<byte[]> document(@PathVariable("documentId") UUID documentId) {
        return file(fileService.readDocument(documentId), true);
    }

    private ResponseEntity<byte[]> file(StudentFileService.StoredContent content, boolean attachment) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(content.fileName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(content.bytes());
    }
}
