package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.*;
import com.kvs.student.enums.StudentStatus;
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
     * List students by tenant and branch (read-only for the school nurse, so they can find a student's record).
     * The tenant must be the caller's own unless the caller is a super administrator.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK', 'CLASS_TEACHER', 'SCHOOL_PRINCIPAL', 'SCHOOL_NURSE') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK', 'ROLE_CLASS_TEACHER', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SCHOOL_NURSE')")
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
     * Personal and contact details of the student (name, date of birth, admission details, address, medication, GP, collection)
     */
    @PutMapping("/{studentId}/profile")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'OFFICE_CLERK', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_OFFICE_CLERK', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateProfile(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        log.info("Request to update profile details for student: {}", studentId);
        StudentResponse response = studentService.updateProfile(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Student profile updated successfully", response));
    }

    /**
     * Guardians and their contact details: the list sent replaces the student's guardians
     */
    @PutMapping("/{studentId}/guardians")
    @PreAuthorize("hasAnyRole('REGISTRAR', 'OFFICE_CLERK', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_REGISTRAR', 'ROLE_OFFICE_CLERK', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateGuardians(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody UpdateGuardiansRequest request) {
        log.info("Request to update guardians for student: {}", studentId);
        StudentResponse response = studentService.updateGuardians(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Student guardians updated successfully", response));
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

    /**
     * US-03010: Filter student demographic records by class, section, house, and transport route
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER', 'ACADEMIC_HEAD', 'REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_ACADEMIC_HEAD', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> searchStudents(
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "branchId", required = false) String branchId,
            @RequestParam(value = "gradeId", required = false) UUID gradeId,
            @RequestParam(value = "sectionId", required = false) UUID sectionId,
            @RequestParam(value = "houseId", required = false) UUID houseId,
            @RequestParam(value = "transportRouteId", required = false) String transportRouteId,
            @RequestParam(value = "status", required = false) StudentStatus status) {
        log.info("Filtering student demographics for tenant: {}, grade: {}, section: {}", tenantId, gradeId, sectionId);
        List<StudentResponse> response = studentService.filterStudents(tenantId, branchId, gradeId, sectionId, houseId, transportRouteId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03011: Bulk update student contact addresses and guardian mobile numbers
     */
    @PutMapping("/bulk-contact-update")
    @PreAuthorize("hasAnyRole('SCHOOL_NURSE', 'SCHOOL_PRINCIPAL', 'REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK') or hasAnyAuthority('ROLE_SCHOOL_NURSE', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK')")
    public ResponseEntity<ApiResponse<Integer>> bulkUpdateContacts(
            @Valid @RequestBody BulkContactUpdateRequest request) {
        log.info("Bulk updating student contact addresses for {} items", request.getItems().size());
        int updated = studentService.bulkUpdateContacts(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk contacts updated successfully", updated));
    }

    /**
     * US-03014: Manage student photo updates and batch export profile image directories
     */
    @PostMapping("/{studentId}/photo")
    @PreAuthorize("hasAnyRole('ACADEMIC_HEAD', 'CLASS_TEACHER', 'OFFICE_CLERK', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ACADEMIC_HEAD', 'ROLE_CLASS_TEACHER', 'ROLE_OFFICE_CLERK', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudentPhoto(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody UpdatePhotoRequest request) {
        log.info("Updating profile photo for student: {}", studentId);
        StudentResponse response = studentService.updateStudentPhoto(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Student photo updated successfully", response));
    }

    @GetMapping("/export-photo-directory")
    @PreAuthorize("hasAnyRole('ACADEMIC_HEAD', 'CLASS_TEACHER', 'OFFICE_CLERK', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ACADEMIC_HEAD', 'ROLE_CLASS_TEACHER', 'ROLE_OFFICE_CLERK', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PhotoExportItemResponse>>> exportPhotoDirectory(
            @RequestParam("gradeId") UUID gradeId,
            @RequestParam("sectionId") UUID sectionId) {
        log.info("Exporting photo directory for grade: {}, section: {}", gradeId, sectionId);
        List<PhotoExportItemResponse> response = studentService.exportPhotoDirectory(gradeId, sectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03015: Flag student profiles under administrative hold for fee or document default
     */
    @PostMapping("/{studentId}/holds")
    @PreAuthorize("hasAnyRole('SCHOOL_PRINCIPAL', 'SCHOOL_NURSE', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_SCHOOL_PRINCIPAL', 'ROLE_SCHOOL_NURSE', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdministrativeHoldResponse>> placeHold(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody PlaceHoldRequest request) {
        log.info("Placing administrative hold for student: {}", studentId);
        AdministrativeHoldResponse response = studentService.placeHold(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Administrative hold placed successfully", response));
    }

    @PatchMapping("/holds/{holdId}/release")
    @PreAuthorize("hasAnyRole('SCHOOL_PRINCIPAL', 'SCHOOL_NURSE', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_SCHOOL_PRINCIPAL', 'ROLE_SCHOOL_NURSE', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdministrativeHoldResponse>> releaseHold(
            @PathVariable("holdId") UUID holdId,
            @Valid @RequestBody ReleaseHoldRequest request) {
        log.info("Releasing administrative hold: {}", holdId);
        AdministrativeHoldResponse response = studentService.releaseHold(holdId, request);
        return ResponseEntity.ok(ApiResponse.success("Administrative hold released successfully", response));
    }

    @GetMapping("/{studentId}/holds")
    @PreAuthorize("hasAnyRole('SCHOOL_PRINCIPAL', 'SCHOOL_NURSE', 'REGISTRAR', 'SUPER_ADMIN', 'OFFICE_CLERK') or hasAnyAuthority('ROLE_SCHOOL_PRINCIPAL', 'ROLE_SCHOOL_NURSE', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_OFFICE_CLERK')")
    public ResponseEntity<ApiResponse<List<AdministrativeHoldResponse>>> listHolds(
            @PathVariable("studentId") UUID studentId) {
        log.info("Fetching administrative holds for student: {}", studentId);
        List<AdministrativeHoldResponse> response = studentService.listHolds(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03016: Track student height and weight metrics during annual health checkups
     */
    @PostMapping("/{studentId}/health-metrics")
    @PreAuthorize("hasAnyRole('SCHOOL_REGISTRAR', 'REGISTRAR', 'HOUSE_MASTER', 'SCHOOL_NURSE', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_SCHOOL_REGISTRAR', 'ROLE_REGISTRAR', 'ROLE_HOUSE_MASTER', 'ROLE_SCHOOL_NURSE', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<HealthMetricResponse>> recordHealthMetric(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody RecordHealthMetricRequest request) {
        log.info("Recording health metric for student: {}", studentId);
        HealthMetricResponse response = studentService.recordHealthMetric(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Health metric recorded successfully", response));
    }

    @GetMapping("/{studentId}/health-metrics")
    @PreAuthorize("hasAnyRole('SCHOOL_REGISTRAR', 'REGISTRAR', 'HOUSE_MASTER', 'SCHOOL_NURSE', 'SUPER_ADMIN', 'CLASS_TEACHER') or hasAnyAuthority('ROLE_SCHOOL_REGISTRAR', 'ROLE_REGISTRAR', 'ROLE_HOUSE_MASTER', 'ROLE_SCHOOL_NURSE', 'ROLE_SUPER_ADMIN', 'ROLE_CLASS_TEACHER')")
    public ResponseEntity<ApiResponse<List<HealthMetricResponse>>> listHealthMetrics(
            @PathVariable("studentId") UUID studentId) {
        log.info("Fetching health metrics for student: {}", studentId);
        List<HealthMetricResponse> response = studentService.listHealthMetrics(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03017: Record student language preferences and elective course selections
     */
    @PostMapping("/{studentId}/electives")
    @PreAuthorize("hasAnyRole('OFFICE_CLERK', 'ALUMNI_COORDINATOR', 'CLASS_TEACHER', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_OFFICE_CLERK', 'ROLE_ALUMNI_COORDINATOR', 'ROLE_CLASS_TEACHER', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ElectiveSelectionResponse>> recordElectiveSelection(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody RecordElectiveSelectionRequest request) {
        log.info("Recording elective selection for student: {}", studentId);
        ElectiveSelectionResponse response = studentService.recordElectiveSelection(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Elective selection recorded successfully", response));
    }

    @GetMapping("/{studentId}/electives")
    @PreAuthorize("hasAnyRole('OFFICE_CLERK', 'ALUMNI_COORDINATOR', 'CLASS_TEACHER', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_OFFICE_CLERK', 'ROLE_ALUMNI_COORDINATOR', 'ROLE_CLASS_TEACHER', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ElectiveSelectionResponse>>> getElectives(
            @PathVariable("studentId") UUID studentId) {
        log.info("Fetching elective selections for student: {}", studentId);
        List<ElectiveSelectionResponse> response = studentService.getElectives(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * US-03019: Process student section transfer requests with academic head approval
     */
    @PostMapping("/{studentId}/section-transfers")
    @PreAuthorize("hasAnyRole('SCHOOL_NURSE', 'ACADEMIC_HEAD', 'SCHOOL_PRINCIPAL', 'REGISTRAR', 'SUPER_ADMIN', 'CLASS_TEACHER') or hasAnyAuthority('ROLE_SCHOOL_NURSE', 'ROLE_ACADEMIC_HEAD', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_CLASS_TEACHER')")
    public ResponseEntity<ApiResponse<SectionTransferResponse>> proposeSectionTransfer(
            @PathVariable("studentId") UUID studentId,
            @Valid @RequestBody SectionTransferProposalRequest request) {
        log.info("Proposing section transfer for student: {}", studentId);
        SectionTransferResponse response = studentService.proposeSectionTransfer(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section transfer request submitted successfully", response));
    }

    @PatchMapping("/section-transfers/{transferId}/review")
    @PreAuthorize("hasAnyRole('ACADEMIC_HEAD', 'SCHOOL_PRINCIPAL', 'REGISTRAR', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ACADEMIC_HEAD', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SectionTransferResponse>> reviewSectionTransfer(
            @PathVariable("transferId") UUID transferId,
            @Valid @RequestBody ReviewSectionTransferRequest request) {
        log.info("Reviewing section transfer request: {}", transferId);
        SectionTransferResponse response = studentService.reviewSectionTransfer(transferId, request);
        return ResponseEntity.ok(ApiResponse.success("Section transfer reviewed successfully", response));
    }

    @GetMapping("/{studentId}/section-transfers")
    @PreAuthorize("hasAnyRole('SCHOOL_NURSE', 'ACADEMIC_HEAD', 'SCHOOL_PRINCIPAL', 'REGISTRAR', 'SUPER_ADMIN', 'CLASS_TEACHER') or hasAnyAuthority('ROLE_SCHOOL_NURSE', 'ROLE_ACADEMIC_HEAD', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_REGISTRAR', 'ROLE_SUPER_ADMIN', 'ROLE_CLASS_TEACHER')")
    public ResponseEntity<ApiResponse<List<SectionTransferResponse>>> listSectionTransfers(
            @PathVariable("studentId") UUID studentId) {
        log.info("Fetching section transfers for student: {}", studentId);
        List<SectionTransferResponse> response = studentService.listSectionTransfers(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
