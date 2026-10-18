package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.DisciplinaryIncidentResponse;
import com.kvs.student.dto.RecordDisciplinaryIncidentRequest;
import com.kvs.student.service.DisciplinaryService;
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
 * Controller managing Student Disciplinary Incidents and Counseling Logs.
 * Covers user stories:
 * - US-03008: Track student disciplinary incidents and record counseling intervention logs
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/students/{studentId}/disciplinary")
public class DisciplinaryIncidentController {

    @Autowired
    private DisciplinaryService disciplinaryService;

    /**
     * US-03008: Track student disciplinary incidents and record counseling intervention logs
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CLASS_TEACHER', 'SCHOOL_PRINCIPAL', 'VICE_PRINCIPAL', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_VICE_PRINCIPAL', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DisciplinaryIncidentResponse>> recordIncident(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody RecordDisciplinaryIncidentRequest request) {
        log.info("Request to record disciplinary incident for student: {}", studentId);
        DisciplinaryIncidentResponse response = disciplinaryService.recordIncident(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Disciplinary incident recorded successfully", response));
    }

    /**
     * List all disciplinary incidents and counseling records for a student
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CLASS_TEACHER', 'SCHOOL_PRINCIPAL', 'VICE_PRINCIPAL', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_VICE_PRINCIPAL', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DisciplinaryIncidentResponse>>> listIncidents(
            @PathVariable("studentId") UUID studentId) {
        log.info("Request to fetch disciplinary incidents for student: {}", studentId);
        List<DisciplinaryIncidentResponse> response = disciplinaryService.listIncidents(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
