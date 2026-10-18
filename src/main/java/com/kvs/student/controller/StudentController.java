package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.*;
import com.kvs.student.service.StudentService;
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
 * Controller managing Student Lifecycle and Information System.
 * Covers user stories:
 * - US-03000: Generate permanent non-reusable Student UID upon enrollment confirmation
 * - US-03001: Update student medical alerts, blood groups, and emergency contacts in real time
 * - US-03002: Link multi-sibling profiles under a single consolidated parent user account
 * - US-03003: Assign enrolled students to specific academic sections and school houses
 * - US-03004: Design and print barcoded student id cards with embedded qr verification codes
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * US-03000: Generate permanent non-reusable Student UID upon enrollment confirmation
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK')")
    public ResponseEntity<ApiResponse<StudentResponse>> enrollStudent(
            @Valid @RequestBody EnrollStudentRequest request) {
        log.info("Request to enroll student: {} {}", request.getFirstName(), request.getLastName());
        StudentResponse response = studentService.enrollStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student enrolled successfully with permanent UID", response));
    }

    /**
     * Retrieve student by primary ID
     */
    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'CLASS_TEACHER', 'SCHOOL_PRINCIPAL', 'SCHOOL_NURSE') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_CLASS_TEACHER', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SCHOOL_NURSE')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @PathVariable("studentId") UUID studentId) {
        log.info("Request to fetch student by ID: {}", studentId);
        StudentResponse response = studentService.getStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieve student by permanent Student UID
     */
    @GetMapping("/uid/{studentUid}")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'CLASS_TEACHER', 'SCHOOL_PRINCIPAL', 'SCHOOL_NURSE') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_CLASS_TEACHER', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SCHOOL_NURSE')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByUid(
            @PathVariable("studentUid") String studentUid) {
        log.info("Request to fetch student by UID: {}", studentUid);
        StudentResponse response = studentService.getStudentByUid(studentUid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * List students by tenant and branch
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'CLASS_TEACHER', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_CLASS_TEACHER', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("branchId") String branchId) {
        log.info("Request to list students for tenant: {}, branch: {}", tenantId, branchId);
        List<StudentResponse> response = studentService.listStudents(tenantId, branchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03001: Update student medical alerts, blood groups, and emergency contacts in real time
     */
    @PutMapping("/{studentId}/medical")
    @PreAuthorize("hasAnyRole('SCHOOL_NURSE', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_SCHOOL_NURSE', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateMedicalAlerts(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody UpdateMedicalAlertsRequest request) {
        log.info("Request to update medical alerts for student: {}", studentId);
        StudentResponse response = studentService.updateMedicalAlerts(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Student medical alerts updated successfully", response));
    }

    /**
     * US-03002: Link multi-sibling profiles under a single consolidated parent user account
     */
    @PostMapping("/{studentId}/siblings")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK')")
    public ResponseEntity<ApiResponse<StudentResponse>> linkSibling(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody LinkSiblingRequest request) {
        log.info("Request to link sibling profile for student: {}", studentId);
        StudentResponse response = studentService.linkSibling(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Student sibling linked successfully", response));
    }

    /**
     * US-03003: Assign enrolled students to specific academic sections and school houses
     */
    @PutMapping("/{studentId}/allocation")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'CLASS_TEACHER', 'SUPER_ADMIN', 'HOUSE_MASTER') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_CLASS_TEACHER', 'ROLE_SUPER_ADMIN', 'ROLE_HOUSE_MASTER')")
    public ResponseEntity<ApiResponse<StudentResponse>> assignSectionAndHouse(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody AssignSectionHouseRequest request) {
        log.info("Request to assign section and house for student: {}", studentId);
        StudentResponse response = studentService.assignSectionAndHouse(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Student allocated to section and house successfully", response));
    }

    /**
     * US-03004: Design and print barcoded student id cards with embedded qr verification codes
     */
    @GetMapping("/{studentId}/id-card")
    @PreAuthorize("hasAnyRole('OFFICE_CLERK', 'REGISTRAR', 'SUPER_ADMIN', 'CLASS_TEACHER') or hasAnyAuthority('ROLE_OFFICE_CLERK', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_CLASS_TEACHER')")
    public ResponseEntity<ApiResponse<IdCardResponse>> generateIdCard(
            @PathVariable("studentId") UUID studentId) {
        log.info("Request to generate ID card for student: {}", studentId);
        IdCardResponse response = studentService.generateIdCard(studentId);
        return ResponseEntity.ok(ApiResponse.success("ID card data generated successfully", response));
    }
}
