package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.CertificateResponse;
import com.kvs.student.dto.IssueBonafideCertificateRequest;
import com.kvs.student.dto.IssueTransferCertificateRequest;
import com.kvs.student.service.CertificateService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller managing official student certificates.
 * Covers user stories:
 * - US-03006: Generate official transfer certificates (TC) with dual administrative sign-off
 * - US-03007: Issue digital bonafide and character certificates with tamper-evident QR codes
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/students/{studentId}/certificates")
public class StudentCertificateController {

    @Autowired
    private CertificateService certificateService;

    /**
     * US-03006: Generate official transfer certificates (TC) with dual administrative sign-off
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('SCHOOL_PRINCIPAL', 'VICE_PRINCIPAL', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_SCHOOL_PRINCIPAL', 'ROLE_VICE_PRINCIPAL', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CertificateResponse>> issueTransferCertificate(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody IssueTransferCertificateRequest request) {
        log.info("Request to issue Transfer Certificate for student: {}", studentId);
        CertificateResponse response = certificateService.issueTransferCertificate(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer certificate issued successfully with dual sign-off", response));
    }

    /**
     * US-03007: Issue digital bonafide and character certificates with tamper-evident QR codes
     */
    @PostMapping("/bonafide")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<CertificateResponse>> issueBonafideCertificate(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody IssueBonafideCertificateRequest request) {
        log.info("Request to issue {} certificate for student: {}", request.getCertificateType(), studentId);
        CertificateResponse response = certificateService.issueBonafideCertificate(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Certificate issued successfully with verification QR code", response));
    }

    /**
     * List all certificates issued for a student
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL', 'CLASS_TEACHER') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_CLASS_TEACHER')")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> listCertificates(
            @PathVariable("studentId") UUID studentId) {
        log.info("Request to list certificates for student: {}", studentId);
        List<CertificateResponse> response = certificateService.listCertificates(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
