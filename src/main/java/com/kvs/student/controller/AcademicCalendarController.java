package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.AcademicCalendarResponse;
import com.kvs.student.dto.SaveAcademicTermsRequest;
import com.kvs.student.dto.UpdateAcademicSessionRequest;
import com.kvs.student.entity.AcademicSession;
import com.kvs.student.entity.AcademicTerm;
import com.kvs.student.service.AcademicCalendarService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Academic years and terms: read by every signed-in user (Calendar → Term dates), edited by the registrar,
 * principals and super administrators (Settings → Academic setup, "Manage term dates").
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/academic")
public class AcademicCalendarController {

    private static final String EDITORS = "hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'SCHOOL_PRINCIPAL', 'PRINCIPAL')"
            + " or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_PRINCIPAL')";

    @Autowired
    private AcademicCalendarService calendarService;

    @GetMapping("/calendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AcademicCalendarResponse>> calendar(@RequestParam("tenantId") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(calendarService.calendar(tenantId)));
    }

    @PutMapping("/sessions/{academicYearId}")
    @PreAuthorize(EDITORS)
    public ResponseEntity<ApiResponse<AcademicSession>> updateSession(@PathVariable("academicYearId") UUID academicYearId,
                                                                      @Valid @RequestBody UpdateAcademicSessionRequest request) {
        log.info("Request to update academic session {}", academicYearId);
        return ResponseEntity.ok(ApiResponse.success("Academic year saved", calendarService.updateSession(academicYearId, request)));
    }

    @PutMapping("/sessions/{academicYearId}/terms")
    @PreAuthorize(EDITORS)
    public ResponseEntity<ApiResponse<List<AcademicTerm>>> saveTerms(@PathVariable("academicYearId") UUID academicYearId,
                                                                     @Valid @RequestBody SaveAcademicTermsRequest request) {
        log.info("Request to save terms of academic session {}", academicYearId);
        return ResponseEntity.ok(ApiResponse.success("Term dates saved", calendarService.saveTerms(academicYearId, request)));
    }
}
