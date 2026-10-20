package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.*;
import com.kvs.student.entity.AcademicSession;
import com.kvs.student.entity.Grade;
import com.kvs.student.entity.House;
import com.kvs.student.entity.Section;
import com.kvs.student.service.AcademicHierarchyService;
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
 * Controller managing Academic Hierarchy (Sessions, Grades, Sections, Houses)
 * and Annual Batch Promotions.
 * Covers user stories:
 * - US-03005: Execute annual batch promotions to advance eligible students to the next grade
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/academic")
public class AcademicHierarchyController {

    @Autowired
    private AcademicHierarchyService academicService;

    @Autowired
    private StudentService studentService;

    /**
     * Create academic session
     */
    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicSession>> createSession(
            @Valid @RequestBody CreateAcademicSessionRequest request) {
        log.info("Request to create academic session: {}", request.getSessionName());
        AcademicSession session = academicService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Academic session created successfully", session));
    }

    /**
     * List academic sessions
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<List<AcademicSession>>> listSessions(
            @RequestParam("tenantId") String tenantId) {
        List<AcademicSession> sessions = academicService.listSessions(tenantId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Create grade
     */
    @PostMapping("/grades")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Grade>> createGrade(
            @Valid @RequestBody CreateGradeRequest request) {
        log.info("Request to create grade: {}", request.getGradeName());
        Grade grade = academicService.createGrade(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade created successfully", grade));
    }

    /**
     * List grades
     */
    @GetMapping("/grades")
    // Class and section names are reference data for every teaching screen (Academics mark entry, broadsheets, attendance registers)
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL', 'CLASS_TEACHER', 'SUBJECT_TEACHER', 'EXAM_CONTROLLER', 'VICE_PRINCIPAL', 'ACADEMIC_AUDITOR', 'ACADEMIC_HEAD')")
    public ResponseEntity<ApiResponse<List<Grade>>> listGrades(
            @RequestParam("tenantId") String tenantId) {
        List<Grade> grades = academicService.listGrades(tenantId);
        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    /**
     * Create section
     */
    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Section>> createSection(
            @Valid @RequestBody CreateSectionRequest request) {
        log.info("Request to create section: {}", request.getSectionName());
        Section section = academicService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created successfully", section));
    }

    /**
     * List sections for grade
     */
    @GetMapping("/sections")
    // Class and section names are reference data for every teaching screen (Academics mark entry, broadsheets, attendance registers)
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL', 'CLASS_TEACHER', 'SUBJECT_TEACHER', 'EXAM_CONTROLLER', 'VICE_PRINCIPAL', 'ACADEMIC_AUDITOR', 'ACADEMIC_HEAD')")
    public ResponseEntity<ApiResponse<List<Section>>> listSections(
            @RequestParam("gradeId") UUID gradeId) {
        List<Section> sections = academicService.listSections(gradeId);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    /**
     * Create house
     */
    @PostMapping("/houses")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<House>> createHouse(
            @Valid @RequestBody CreateHouseRequest request) {
        log.info("Request to create house: {}", request.getHouseName());
        House house = academicService.createHouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("House created successfully", house));
    }

    /**
     * List houses
     */
    @GetMapping("/houses")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL', 'CLASS_TEACHER', 'HOUSE_MASTER') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_CLASS_TEACHER', 'ROLE_HOUSE_MASTER')")
    public ResponseEntity<ApiResponse<List<House>>> listHouses(
            @RequestParam("tenantId") String tenantId) {
        List<House> houses = academicService.listHouses(tenantId);
        return ResponseEntity.ok(ApiResponse.success(houses));
    }

    @Autowired
    private com.kvs.student.service.StudentActivityService activityService;

    /**
     * US-03005: Execute annual batch promotions to advance eligible students to the next grade
     */
    @PostMapping("/batch-promotion")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<BatchPromotionResponse>> batchPromoteStudents(
            @Valid @RequestBody BatchPromotionRequest request) {
        log.info("Request to execute batch promotion from grade {} to grade {}", request.getFromGradeId(), request.getToGradeId());
        BatchPromotionResponse response = studentService.batchPromoteStudents(request);
        return ResponseEntity.ok(ApiResponse.success("Batch promotion executed successfully", response));
    }

    /**
     * US-03018: Generate class enrollment roll lists for daily attendance verification
     */
    @GetMapping("/sections/{sectionId}/roll-list")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER', 'ACADEMIC_HEAD', 'REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_ACADEMIC_HEAD', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<ClassRollListResponse>> getSectionRollList(
            @PathVariable("sectionId") UUID sectionId) {
        log.info("Request to generate class roll list for section: {}", sectionId);
        ClassRollListResponse response = studentService.getSectionRollList(sectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03012: Extracurricular and house points aggregation
     */
    @GetMapping("/houses/{houseId}/points")
    @PreAuthorize("hasAnyRole('HOUSE_MASTER', 'CLASS_TEACHER', 'ACADEMIC_HEAD', 'REGISTRAR', 'SUPER_ADMIN', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_HOUSE_MASTER', 'ROLE_CLASS_TEACHER', 'ROLE_ACADEMIC_HEAD', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_PRINCIPAL')")
    public ResponseEntity<ApiResponse<Integer>> getHousePoints(
            @PathVariable("houseId") UUID houseId) {
        log.info("Request to fetch total points for house: {}", houseId);
        Integer points = activityService.getHouseTotalPoints(houseId);
        return ResponseEntity.ok(ApiResponse.success(points));
    }
}
