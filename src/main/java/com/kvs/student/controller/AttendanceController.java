package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.dto.*;
import com.kvs.student.enums.AttendanceStatus;
import com.kvs.student.service.AttendanceService;
import com.kvs.student.service.TenantAccessGuard;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily student attendance (one register a day, or AM and PM registers, per the school's setting), built on the class roll list of US-03018.
 * School-wide lists default to the caller's tenant (the token's tenantId claim); a SUPER_ADMIN passes tenantId.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private static final String VIEW = "hasAnyRole('CLASS_TEACHER', 'ACADEMIC_HEAD', 'VICE_PRINCIPAL', 'SCHOOL_PRINCIPAL', 'REGISTRAR', 'OFFICE_CLERK', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_ACADEMIC_HEAD', 'ROLE_VICE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_REGISTRAR', 'ROLE_OFFICE_CLERK', 'ROLE_SUPER_ADMIN')";
    private static final String MARK = "hasAnyRole('CLASS_TEACHER', 'ACADEMIC_HEAD', 'VICE_PRINCIPAL', 'SCHOOL_PRINCIPAL', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_ACADEMIC_HEAD', 'ROLE_VICE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SUPER_ADMIN')";
    private static final String FOLLOW_UP = "hasAnyRole('CLASS_TEACHER', 'OFFICE_CLERK', 'VICE_PRINCIPAL', 'SCHOOL_PRINCIPAL', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_CLASS_TEACHER', 'ROLE_OFFICE_CLERK', 'ROLE_VICE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SUPER_ADMIN')";

    /** Who may switch between one register a day and morning and afternoon registers */
    private static final String SETTINGS = "hasAnyRole('SCHOOL_PRINCIPAL', 'PRINCIPAL', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_SCHOOL_PRINCIPAL', 'ROLE_PRINCIPAL', 'ROLE_SUPER_ADMIN')";

    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private TenantAccessGuard tenantGuard;

    /** The school's register mode (DAILY or AM_PM); any signed-in user of the school may read it. */
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceSettingsResponse>> getSettings(@RequestParam(value = "tenantId", required = false) String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getSettings(tenantId)));
    }

    /** One register a day (DAILY) or morning and afternoon registers (AM_PM). Figures follow the mode at once; marks already taken are kept. */
    @PutMapping("/settings")
    @PreAuthorize(SETTINGS)
    public ResponseEntity<ApiResponse<AttendanceSettingsResponse>> saveSettings(@RequestParam(value = "tenantId", required = false) String tenantId,
                                                                               @Valid @RequestBody SaveAttendanceSettingsRequest request) {
        log.info("Request to set attendance registers to {}", request.getMode());
        return ResponseEntity.ok(ApiResponse.success("Attendance settings saved", attendanceService.saveSettings(tenantId, request)));
    }

    /** A section's register for a day (default today): AM and PM marks of every student on roll. */
    @GetMapping("/sections/{sectionId}/register")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<AttendanceRegisterResponse>> getRegister(
            @PathVariable("sectionId") UUID sectionId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request for attendance register of section {} on {}", sectionId, date);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getRegister(sectionId, date)));
    }

    /** Submit or correct the day register (or the AM or PM register); corrections older than 7 days need a principal or vice principal. */
    @PutMapping("/sections/{sectionId}/register")
    @PreAuthorize(MARK)
    public ResponseEntity<ApiResponse<AttendanceRegisterResponse>> saveRegister(
            @PathVariable("sectionId") UUID sectionId,
            @Valid @RequestBody SaveAttendanceRequest request) {
        log.info("Request to save {} attendance register of section {} on {}", request.getSession(), sectionId, request.getDate());
        return ResponseEntity.ok(ApiResponse.success("Attendance register saved", attendanceService.saveRegister(sectionId, request)));
    }

    /** The school's attendance for a day (default today), following the register mode. */
    @GetMapping("/summary")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getSummary(
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "branchId", required = false) String branchId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        tenantGuard.requireBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getDaySummary(tenantId, branchId, date)));
    }

    /** Daily figures for school days in a range (default the last 14 days). */
    @GetMapping("/trend")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<List<AttendanceSummaryResponse>>> getTrend(
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "branchId", required = false) String branchId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        tenantGuard.requireBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getTrend(tenantId, branchId, from, to)));
    }

    /** Late arrivals and absences (default the last 30 days), newest first. */
    @GetMapping("/absences")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> listAbsences(
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "branchId", required = false) String branchId,
            @RequestParam(value = "gradeId", required = false) UUID gradeId,
            @RequestParam(value = "sectionId", required = false) UUID sectionId,
            @RequestParam(value = "status", required = false) AttendanceStatus status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        tenantGuard.requireBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.listAbsences(tenantId, branchId, gradeId, sectionId, status, from, to)));
    }

    /** Unexcused absences awaiting guardian follow-up (default the last 7 days). */
    @GetMapping("/follow-ups")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> listFollowUps(
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "branchId", required = false) String branchId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "includeResolved", defaultValue = "false") boolean includeResolved) {
        tenantGuard.requireBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.listFollowUps(tenantId, branchId, from, to, includeResolved)));
    }

    /** Record the guardian follow-up of an unexcused absence. */
    @PatchMapping("/records/{attendanceId}/follow-up")
    @PreAuthorize(FOLLOW_UP)
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> updateFollowUp(
            @PathVariable("attendanceId") UUID attendanceId,
            @Valid @RequestBody UpdateFollowUpRequest request) {
        log.info("Request to update follow-up of attendance record {}", attendanceId);
        return ResponseEntity.ok(ApiResponse.success("Follow-up recorded", attendanceService.updateFollowUp(attendanceId, request)));
    }

    /** One student's attendance over a period (default the active academic session to date). */
    @GetMapping("/students/{studentId}")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<StudentAttendanceSummaryResponse>> getStudentSummary(
            @PathVariable("studentId") UUID studentId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getStudentSummary(studentId, from, to)));
    }
}
