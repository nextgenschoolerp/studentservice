package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.ActivityCreditResponse;
import com.kvs.student.dto.LogActivityCreditRequest;
import com.kvs.student.service.StudentActivityService;
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
 * Controller managing student extracurricular participation and house points.
 * Covers user story:
 * - US-03012: Log student extracurricular participation credits and house points
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/students/{studentId}/activities")
public class StudentActivityController {

    @Autowired
    private StudentActivityService activityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOUSE_MASTER', 'CLASS_TEACHER', 'SUPER_ADMIN', 'REGISTRAR') or hasAnyAuthority('ROLE_HOUSE_MASTER', 'ROLE_CLASS_TEACHER', 'ROLE_SUPER_ADMIN', 'ROLE_REGISTRAR')")
    public ResponseEntity<ApiResponse<ActivityCreditResponse>> logActivityCredit(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody LogActivityCreditRequest request) {
        log.info("Request to log activity credit for student: {}", studentId);
        ActivityCreditResponse response = activityService.logActivityCredit(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Activity credit recorded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOUSE_MASTER', 'CLASS_TEACHER', 'SUPER_ADMIN', 'REGISTRAR', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_HOUSE_MASTER', 'ROLE_CLASS_TEACHER', 'ROLE_SUPER_ADMIN', 'ROLE_REGISTRAR', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<List<ActivityCreditResponse>>> listActivityCredits(
            @PathVariable("studentId") UUID studentId) {
        log.info("Request to list activity credits for student: {}", studentId);
        List<ActivityCreditResponse> response = activityService.listActivityCredits(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
