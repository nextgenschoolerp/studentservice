package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.AlumniResponse;
import com.kvs.student.dto.ArchiveAlumniRequest;
import com.kvs.student.service.AlumniService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller managing Permanent Alumni Registry.
 * Covers user stories:
 * - US-03009: Archive graduating student profiles into the permanent alumni registry
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/alumni")
public class AlumniRegistryController {

    @Autowired
    private AlumniService alumniService;

    /**
     * US-03009: Archive graduating student profiles into the permanent alumni registry
     */
    @PostMapping("/archive")
    @PreAuthorize("hasAnyRole('ALUMNI_COORDINATOR', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ALUMNI_COORDINATOR', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AlumniResponse>> archiveAlumni(
            @Valid @RequestBody ArchiveAlumniRequest request) {
        log.info("Request to archive student {} to alumni registry", request.getStudentId());
        AlumniResponse response = alumniService.archiveAlumni(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Graduating student archived to alumni registry successfully", response));
    }

    /**
     * Retrieve permanent alumni profile by student UID
     */
     @GetMapping("/{studentUid}")
     @PreAuthorize("hasAnyRole('ALUMNI_COORDINATOR', 'REGISTRAR', 'SUPER_ADMIN', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_ALUMNI_COORDINATOR', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_PRINCIPAL')")
     public ResponseEntity<ApiResponse<AlumniResponse>> getAlumniByUid(
             @PathVariable("studentUid") String studentUid) {
         log.info("Request to fetch alumni profile for student UID: {}", studentUid);
         AlumniResponse response = alumniService.getAlumniByUid(studentUid);
         return ResponseEntity.ok(ApiResponse.success(response));
     }

    /**
     * US-03013: Verify former student credential requests against historical archive ledgers
     */
    @PostMapping("/credential-verifications")
    @PreAuthorize("hasAnyRole('ALUMNI_COORDINATOR', 'OFFICE_CLERK', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ALUMNI_COORDINATOR', 'ROLE_OFFICE_CLERK', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.student.dto.CredentialVerificationResponse>> submitCredentialVerification(
            @Valid @RequestBody com.kvs.student.dto.SubmitCredentialVerificationRequest request) {
        log.info("Request to submit credential verification for student UID: {}", request.getStudentUid());
        com.kvs.student.dto.CredentialVerificationResponse response = alumniService.submitCredentialVerification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Credential verification request submitted successfully", response));
    }

    @GetMapping("/credential-verifications/{requestId}")
    @PreAuthorize("hasAnyRole('ALUMNI_COORDINATOR', 'OFFICE_CLERK', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ALUMNI_COORDINATOR', 'ROLE_OFFICE_CLERK', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.student.dto.CredentialVerificationResponse>> getCredentialVerification(
            @PathVariable("requestId") java.util.UUID requestId) {
        log.info("Request to fetch credential verification request: {}", requestId);
        com.kvs.student.dto.CredentialVerificationResponse response = alumniService.getCredentialVerification(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/credential-verifications/{requestId}/verify")
    @PreAuthorize("hasAnyRole('ALUMNI_COORDINATOR', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ALUMNI_COORDINATOR', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.student.dto.CredentialVerificationResponse>> reviewCredentialVerification(
            @PathVariable("requestId") java.util.UUID requestId,
            @Valid @RequestBody com.kvs.student.dto.ReviewCredentialVerificationRequest request) {
        log.info("Request to review credential verification request: {}", requestId);
        com.kvs.student.dto.CredentialVerificationResponse response = alumniService.reviewCredentialVerification(requestId, request);
        return ResponseEntity.ok(ApiResponse.success("Credential verification reviewed successfully", response));
    }
}
