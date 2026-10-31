package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ForbiddenException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.*;
import com.kvs.student.entity.*;
import com.kvs.student.enums.*;
import com.kvs.student.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Daily student attendance: one register a day or AM and PM registers per section (the school's register mode), absence reasons, guardian follow-up of unexcused
 * absences, the school's day figure and trend, and each student's attendance to date.
 * Builds on the class roll list (US-03018): a register lists the section's active students in roll-number order.
 */
@Service
@Slf4j
public class AttendanceService {

    /** Class teachers may correct a register up to this many days back; principals at any time */
    static final int TEACHER_CORRECTION_DAYS = 7;
    static final int MAX_RANGE_DAYS = 366;
    private static final Set<StudentStatus> ON_ROLL = EnumSet.of(StudentStatus.ACTIVE, StudentStatus.PROMOTED);
    private static final Set<String> LATE_CORRECTION_ROLES = Set.of("ROLE_SCHOOL_PRINCIPAL", "ROLE_VICE_PRINCIPAL", "ROLE_SUPER_ADMIN");

    @Autowired private StudentAttendanceRepository attendanceRepository;
    @Autowired private AttendanceRegisterRepository registerRepository;
    @Autowired private StudentMasterRepository studentRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private GradeRepository gradeRepository;
    @Autowired private AcademicSessionRepository sessionRepository;
    @Autowired private StudentAuditService auditService;
    @Autowired private TenantAccessGuard tenantGuard;
    @Autowired private AttendanceSettingsRepository settingsRepository;

    // ------------------------------------------------------------------ settings

    /** The school's register mode; one register a day until a principal chooses morning and afternoon registers. */
    public AttendanceRegisterMode modeOf(String tenantId) {
        return settingsRepository.findById(tenantId).map(AttendanceSettings::getMode).orElse(AttendanceRegisterMode.DAILY);
    }

    @Transactional(readOnly = true)
    public AttendanceSettingsResponse getSettings(String tenantId) {
        String tenant = resolveTenant(tenantId);
        return settingsRepository.findById(tenant).map(this::toSettings)
                .orElse(AttendanceSettingsResponse.builder().tenantId(tenant).mode(AttendanceRegisterMode.DAILY).sessionsPerDay(1).build());
    }

    @Transactional
    public AttendanceSettingsResponse saveSettings(String tenantId, SaveAttendanceSettingsRequest request) {
        String tenant = resolveTenant(tenantId);
        AttendanceSettings s = settingsRepository.findById(tenant).orElse(AttendanceSettings.builder().tenantId(tenant).build());
        AttendanceRegisterMode before = s.getMode() != null ? s.getMode() : AttendanceRegisterMode.DAILY;
        AttendanceRegisterMode mode;
        try { mode = AttendanceRegisterMode.valueOf(request.getMode().trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new BadRequestException("Unknown register mode " + request.getMode() + ": use DAILY (one register a day) or AM_PM (morning and afternoon registers)"); }
        s.setMode(mode);
        AttendanceSettings saved = settingsRepository.save(s);
        auditService.logSuccess("tenant:" + tenant, "UPDATE_ATTENDANCE_MODE", "Attendance registers: " + before + " → " + mode);
        return toSettings(saved);
    }

    private AttendanceSettingsResponse toSettings(AttendanceSettings s) {
        return AttendanceSettingsResponse.builder().tenantId(s.getTenantId()).mode(s.getMode()).sessionsPerDay(s.getMode().sessionsPerDay())
                .updatedBy(s.getUpdatedBy() != null ? s.getUpdatedBy() : s.getCreatedBy())
                .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt()).build();
    }

    /** Marks that count in this mode: with one register a day only the day register (stored as AM). */
    private static boolean counts(AttendanceRegisterMode mode, AttendanceSession session) {
        return mode == AttendanceRegisterMode.AM_PM || session == AttendanceSession.AM;
    }

    // ------------------------------------------------------------------ register

    @Transactional(readOnly = true)
    public AttendanceRegisterResponse getRegister(UUID sectionId, LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now();
        Section section = findSection(sectionId);
        Grade grade = findGrade(section);
        return buildRegister(grade, section, day);
    }

    @Transactional
    public AttendanceRegisterResponse saveRegister(UUID sectionId, SaveAttendanceRequest request) {
        Section section = findSection(sectionId);
        Grade grade = findGrade(section);
        LocalDate today = LocalDate.now();
        if (request.getDate().isAfter(today)) {
            throw new BadRequestException("Attendance can't be marked for a future date");
        }
        if (request.getDate().isBefore(today.minusDays(TEACHER_CORRECTION_DAYS))
                && SecurityUtils.getCurrentUserRoles().stream().noneMatch(LATE_CORRECTION_ROLES::contains)) {
            throw new ForbiddenException("Registers older than " + TEACHER_CORRECTION_DAYS + " days can only be corrected by a principal or vice principal");
        }

        AttendanceRegisterMode mode = modeOf(grade.getTenantId());
        if (mode == AttendanceRegisterMode.DAILY) {
            if (request.getSession() == AttendanceSession.PM) {
                throw new BadRequestException("This school takes one register a day; there is no afternoon register. A principal can switch to morning and afternoon registers in Settings → Academic setup.");
            }
            request.setSession(AttendanceSession.AM);
        } else if (request.getSession() == null) {
            throw new BadRequestException("Session (AM or PM) is required: this school takes morning and afternoon registers");
        }

        Map<UUID, StudentMaster> roll = onRoll(sectionId).stream().collect(Collectors.toMap(StudentMaster::getStudentId, Function.identity()));
        Set<UUID> seen = new HashSet<>();
        List<String> problems = new ArrayList<>();
        for (SaveAttendanceRequest.Entry e : request.getEntries()) {
            if (!seen.add(e.getStudentId())) problems.add("student " + e.getStudentId() + " is listed twice");
            else if (!roll.containsKey(e.getStudentId())) problems.add("student " + e.getStudentId() + " is not on the roll of this section");
            else if (e.getStatus() == AttendanceStatus.EXCUSED && (e.getReason() == null || e.getReason().isBlank()))
                problems.add(roll.get(e.getStudentId()).getStudentUid() + ": an excused absence needs a reason");
        }
        if (!problems.isEmpty()) {
            throw new BadRequestException("Register not saved: " + String.join("; ", problems));
        }

        String user = SecurityUtils.getCurrentUserEmail().orElse("system");
        Map<UUID, StudentAttendance> existing = attendanceRepository.findByStudentIdInAndAttendanceDate(roll.keySet(), request.getDate()).stream()
                .filter(a -> a.getSession() == request.getSession())
                .collect(Collectors.toMap(StudentAttendance::getStudentId, Function.identity()));
        int changed = 0;
        for (SaveAttendanceRequest.Entry e : request.getEntries()) {
            StudentMaster s = roll.get(e.getStudentId());
            StudentAttendance a = existing.get(e.getStudentId());
            String reason = e.getReason() == null || e.getReason().isBlank() ? null : e.getReason().trim();
            if (a == null) {
                a = StudentAttendance.builder().tenantId(s.getTenantId()).branchId(s.getBranchId()).studentId(s.getStudentId())
                        .gradeId(grade.getGradeId()).sectionId(sectionId).attendanceDate(request.getDate()).session(request.getSession()).build();
            } else if (a.getStatus() == e.getStatus() && Objects.equals(a.getReason(), reason)) {
                continue;
            }
            boolean nowUnexcused = e.getStatus() == AttendanceStatus.UNEXCUSED;
            if (nowUnexcused && a.getStatus() != AttendanceStatus.UNEXCUSED) {
                a.setFollowUpStatus(FollowUpStatus.OPEN);
            } else if (!nowUnexcused) {
                a.setFollowUpStatus(null);
                a.setFollowUpNote(null);
                a.setFollowUpBy(null);
                a.setFollowUpAt(null);
            }
            a.setStatus(e.getStatus());
            a.setReason(reason);
            a.setMarkedBy(user);
            attendanceRepository.save(a);
            changed++;
        }

        Instant now = Instant.now();
        AttendanceRegister register = registerRepository.findBySectionIdAndRegisterDateAndSession(sectionId, request.getDate(), request.getSession())
                .orElse(null);
        if (register == null) {
            registerRepository.save(AttendanceRegister.builder().tenantId(grade.getTenantId()).sectionId(sectionId)
                    .registerDate(request.getDate()).session(request.getSession()).submittedBy(user).submittedAt(now).build());
        } else if (changed > 0) {
            register.setLastChangedBy(user);
            register.setLastChangedAt(now);
            registerRepository.save(register);
        }
        auditService.logSuccess("section:" + sectionId, register == null ? "SUBMIT_ATTENDANCE_REGISTER" : "CORRECT_ATTENDANCE_REGISTER",
                grade.getGradeName() + "-" + section.getSectionName() + " " + request.getDate() + " " + (mode == AttendanceRegisterMode.DAILY ? "day register" : request.getSession()) + ": " + changed + " mark(s) saved");
        return buildRegister(grade, section, request.getDate());
    }

    // ------------------------------------------------------------------ follow-up

    @Transactional
    public AttendanceRecordResponse updateFollowUp(UUID attendanceId, UpdateFollowUpRequest request) {
        StudentAttendance a = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentAttendance", "attendanceId", attendanceId));
        tenantGuard.requireTenantRecord(a.getTenantId(), "StudentAttendance", "attendanceId", attendanceId);
        if (a.getStatus() != AttendanceStatus.UNEXCUSED) {
            throw new ConflictException("Only unexcused absences have a guardian follow-up; this mark is " + a.getStatus());
        }
        if (request.getFollowUpStatus() == FollowUpStatus.OPEN) {
            throw new BadRequestException("Follow-up status must be GUARDIAN_CONTACTED or RESOLVED");
        }
        a.setFollowUpStatus(request.getFollowUpStatus());
        a.setFollowUpNote(request.getNote().trim());
        a.setFollowUpBy(SecurityUtils.getCurrentUserEmail().orElse("system"));
        a.setFollowUpAt(Instant.now());
        attendanceRepository.save(a);
        auditService.logSuccess("student:" + a.getStudentId(), "ATTENDANCE_FOLLOW_UP",
                "Absence " + a.getAttendanceDate() + " " + a.getSession() + " → " + request.getFollowUpStatus());
        return toRecords(List.of(a)).get(0);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listFollowUps(String tenantId, String branchId, LocalDate from, LocalDate to, boolean includeResolved) {
        String tenant = resolveTenant(tenantId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(6);
        checkRange(start, end);
        AttendanceRegisterMode mode = modeOf(tenant);
        List<StudentAttendance> rows = attendanceRepository.findByTenantIdAndStatusAndAttendanceDateBetween(tenant, AttendanceStatus.UNEXCUSED, start, end).stream()
                .filter(a -> counts(mode, a.getSession()))
                .filter(a -> branchId == null || branchId.equalsIgnoreCase(a.getBranchId()))
                .filter(a -> includeResolved || a.getFollowUpStatus() != FollowUpStatus.RESOLVED)
                .collect(Collectors.toList());
        return sorted(toRecords(rows));
    }

    // ------------------------------------------------------------------ absence history

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listAbsences(String tenantId, String branchId, UUID gradeId, UUID sectionId,
                                                       AttendanceStatus status, LocalDate from, LocalDate to) {
        String tenant = resolveTenant(tenantId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        checkRange(start, end);
        if (status == AttendanceStatus.PRESENT) {
            throw new BadRequestException("Absence history lists LATE, EXCUSED and UNEXCUSED marks");
        }
        AttendanceRegisterMode mode = modeOf(tenant);
        List<StudentAttendance> rows = attendanceRepository.findByTenantIdAndAttendanceDateBetween(tenant, start, end).stream()
                .filter(a -> counts(mode, a.getSession()))
                .filter(a -> a.getStatus() != AttendanceStatus.PRESENT)
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> branchId == null || branchId.equalsIgnoreCase(a.getBranchId()))
                .filter(a -> gradeId == null || gradeId.equals(a.getGradeId()))
                .filter(a -> sectionId == null || sectionId.equals(a.getSectionId()))
                .collect(Collectors.toList());
        return sorted(toRecords(rows));
    }

    // ------------------------------------------------------------------ school day and trend

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getDaySummary(String tenantId, String branchId, LocalDate date) {
        String tenant = resolveTenant(tenantId);
        LocalDate day = date != null ? date : LocalDate.now();
        return summarise(tenant, branchId, day, day).get(0);
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getTrend(String tenantId, String branchId, LocalDate from, LocalDate to) {
        String tenant = resolveTenant(tenantId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(13);
        checkRange(start, end);
        // Only school days: days on which at least one register that counts in this mode was submitted
        return summarise(tenant, branchId, start, end).stream().filter(d -> d.getRegistersSubmitted() > 0).collect(Collectors.toList());
    }

    /**
     * One summary per day from start to end, from today's roll (students on roll now). One register a day: the day marks (students).
     * Morning and afternoon registers: both sessions (half-day sessions; a student counts twice).
     */
    private List<AttendanceSummaryResponse> summarise(String tenant, String branchId, LocalDate start, LocalDate end) {
        AttendanceRegisterMode mode = modeOf(tenant);
        int perDay = mode.sessionsPerDay();
        List<StudentMaster> students = studentRepository.findByTenantId(tenant).stream()
                .filter(s -> ON_ROLL.contains(s.getStudentStatus()))
                .filter(s -> branchId == null || branchId.equalsIgnoreCase(s.getBranchId()))
                .collect(Collectors.toList());
        Set<UUID> onRoll = students.stream().map(StudentMaster::getStudentId).collect(Collectors.toSet());
        Set<UUID> sections = students.stream().map(StudentMaster::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<LocalDate, List<StudentAttendance>> marks = attendanceRepository.findByTenantIdAndAttendanceDateBetween(tenant, start, end).stream()
                .filter(a -> onRoll.contains(a.getStudentId()))
                .collect(Collectors.groupingBy(StudentAttendance::getAttendanceDate));
        Map<LocalDate, List<AttendanceRegister>> registers = registerRepository.findByTenantIdAndRegisterDateBetween(tenant, start, end).stream()
                .filter(r -> sections.contains(r.getSectionId()))
                .collect(Collectors.groupingBy(AttendanceRegister::getRegisterDate));

        List<AttendanceSummaryResponse> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<StudentAttendance> day = marks.getOrDefault(d, List.of());
            List<StudentAttendance> taken = day.stream().filter(a -> counts(mode, a.getSession())).toList();
            int present = count(taken, AttendanceStatus.PRESENT) + count(taken, AttendanceStatus.LATE);
            List<AttendanceRegister> regs = registers.getOrDefault(d, List.of());
            days.add(AttendanceSummaryResponse.builder()
                    .date(d)
                    .mode(mode)
                    .sessionsPerDay(perDay)
                    .totalStudents(students.size())
                    .possible(students.size() * perDay)
                    .present(present)
                    .late(count(taken, AttendanceStatus.LATE))
                    .excused(count(taken, AttendanceStatus.EXCUSED))
                    .unexcused(count(taken, AttendanceStatus.UNEXCUSED))
                    .unmarked(students.size() * perDay - taken.size())
                    .presentRate(rate(present, taken.size()))
                    .registersExpected(sections.size() * perDay)
                    .registersSubmitted((int) regs.stream().filter(r -> counts(mode, r.getSession())).count())
                    .amRegistersSubmitted((int) regs.stream().filter(r -> r.getSession() == AttendanceSession.AM).count())
                    .pmRegistersSubmitted((int) regs.stream().filter(r -> r.getSession() == AttendanceSession.PM).count())
                    .openFollowUps((int) taken.stream().filter(a -> a.getStatus() == AttendanceStatus.UNEXCUSED && a.getFollowUpStatus() != FollowUpStatus.RESOLVED).count())
                    .build());
        }
        return days;
    }

    // ------------------------------------------------------------------ one student

    @Transactional(readOnly = true)
    public StudentAttendanceSummaryResponse getStudentSummary(UUID studentId, LocalDate from, LocalDate to) {
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));
        tenantGuard.requireTenantRecord(student.getTenantId(), "StudentMaster", "studentId", studentId);
        LocalDate today = LocalDate.now();
        LocalDate start = from, end = to;
        String label = null;
        if (start == null) {
            Optional<AcademicSession> session = sessionRepository.findByTenantIdAndIsActiveTrue(student.getTenantId());
            start = session.map(AcademicSession::getStartDate).orElse(today.minusDays(89));
            label = session.map(AcademicSession::getSessionName).orElse(null);
        }
        if (end == null) end = today;
        checkRange(start, end);

        AttendanceRegisterMode mode = modeOf(student.getTenantId());
        List<StudentAttendance> rows = attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDescSessionDesc(studentId, start, end)
                .stream().filter(a -> counts(mode, a.getSession())).toList();
        int present = count(rows, AttendanceStatus.PRESENT), late = count(rows, AttendanceStatus.LATE);
        List<StudentAttendance> todayRows = rows.stream().filter(a -> a.getAttendanceDate().equals(today)).toList();
        return StudentAttendanceSummaryResponse.builder()
                .studentId(studentId)
                .from(start)
                .to(end)
                .periodLabel(label)
                .mode(mode)
                .sessionsMarked(rows.size())
                .present(present)
                .late(late)
                .excused(count(rows, AttendanceStatus.EXCUSED))
                .unexcused(count(rows, AttendanceStatus.UNEXCUSED))
                .attendanceRate(rate(present + late, rows.size()))
                .todayAm(todayRows.stream().filter(a -> a.getSession() == AttendanceSession.AM).map(StudentAttendance::getStatus).findFirst().orElse(null))
                .todayPm(todayRows.stream().filter(a -> a.getSession() == AttendanceSession.PM).map(StudentAttendance::getStatus).findFirst().orElse(null))
                .absences(toRecords(rows.stream().filter(a -> a.getStatus() != AttendanceStatus.PRESENT).toList()))
                .build();
    }

    // ------------------------------------------------------------------ helpers

    private AttendanceRegisterResponse buildRegister(Grade grade, Section section, LocalDate day) {
        List<StudentMaster> roll = onRoll(section.getSectionId());
        Map<UUID, List<StudentAttendance>> marks = attendanceRepository.findBySectionIdAndAttendanceDate(section.getSectionId(), day).stream()
                .collect(Collectors.groupingBy(StudentAttendance::getStudentId));
        Map<AttendanceSession, AttendanceRegister> registers = registerRepository.findBySectionIdAndRegisterDate(section.getSectionId(), day).stream()
                .collect(Collectors.toMap(AttendanceRegister::getSession, Function.identity()));

        List<AttendanceRegisterResponse.StudentRow> rows = roll.stream().map(s -> {
            List<StudentAttendance> m = marks.getOrDefault(s.getStudentId(), List.of());
            return AttendanceRegisterResponse.StudentRow.builder()
                    .studentId(s.getStudentId()).studentUid(s.getStudentUid()).rollNumber(s.getRollNumber())
                    .fullName(s.getFirstName() + " " + s.getLastName())
                    .am(mark(m, AttendanceSession.AM)).pm(mark(m, AttendanceSession.PM))
                    .build();
        }).toList();

        return AttendanceRegisterResponse.builder()
                .gradeId(grade.getGradeId()).gradeName(grade.getGradeName())
                .sectionId(section.getSectionId()).sectionName(section.getSectionName())
                .date(day)
                .mode(modeOf(grade.getTenantId()))
                .am(sessionInfo(rows, AttendanceSession.AM, registers.get(AttendanceSession.AM)))
                .pm(sessionInfo(rows, AttendanceSession.PM, registers.get(AttendanceSession.PM)))
                .students(rows)
                .build();
    }

    private static AttendanceRegisterResponse.Mark mark(List<StudentAttendance> marks, AttendanceSession session) {
        return marks.stream().filter(a -> a.getSession() == session).findFirst()
                .map(a -> AttendanceRegisterResponse.Mark.builder().attendanceId(a.getAttendanceId()).status(a.getStatus())
                        .reason(a.getReason()).followUpStatus(a.getFollowUpStatus()).build())
                .orElse(null);
    }

    private static AttendanceRegisterResponse.SessionInfo sessionInfo(List<AttendanceRegisterResponse.StudentRow> rows, AttendanceSession session, AttendanceRegister register) {
        Map<AttendanceStatus, Long> counts = rows.stream().map(r -> session == AttendanceSession.AM ? r.getAm() : r.getPm())
                .filter(Objects::nonNull).collect(Collectors.groupingBy(AttendanceRegisterResponse.Mark::getStatus, Collectors.counting()));
        int marked = counts.values().stream().mapToInt(Long::intValue).sum();
        return AttendanceRegisterResponse.SessionInfo.builder()
                .submitted(register != null)
                .submittedBy(register != null ? register.getSubmittedBy() : null)
                .submittedAt(register != null ? register.getSubmittedAt() : null)
                .lastChangedBy(register != null ? register.getLastChangedBy() : null)
                .lastChangedAt(register != null ? register.getLastChangedAt() : null)
                .present(counts.getOrDefault(AttendanceStatus.PRESENT, 0L).intValue())
                .late(counts.getOrDefault(AttendanceStatus.LATE, 0L).intValue())
                .excused(counts.getOrDefault(AttendanceStatus.EXCUSED, 0L).intValue())
                .unexcused(counts.getOrDefault(AttendanceStatus.UNEXCUSED, 0L).intValue())
                .unmarked(rows.size() - marked)
                .build();
    }

    /** Marks with the student's name and class, for history and follow-up lists. */
    private List<AttendanceRecordResponse> toRecords(List<StudentAttendance> rows) {
        if (rows.isEmpty()) return new ArrayList<>();
        Map<UUID, StudentMaster> students = studentRepository.findAllById(rows.stream().map(StudentAttendance::getStudentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(StudentMaster::getStudentId, Function.identity()));
        Map<UUID, String> grades = gradeRepository.findAllById(rows.stream().map(StudentAttendance::getGradeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Grade::getGradeId, Grade::getGradeName));
        Map<UUID, String> sections = sectionRepository.findAllById(rows.stream().map(StudentAttendance::getSectionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Section::getSectionId, Section::getSectionName));
        return rows.stream().map(a -> {
            StudentMaster s = students.get(a.getStudentId());
            return AttendanceRecordResponse.builder()
                    .attendanceId(a.getAttendanceId()).studentId(a.getStudentId())
                    .studentUid(s != null ? s.getStudentUid() : null)
                    .fullName(s != null ? s.getFirstName() + " " + s.getLastName() : null)
                    .emergencyContactPhone(s != null ? s.getEmergencyContactPhone() : null)
                    .gradeName(grades.get(a.getGradeId())).sectionName(sections.get(a.getSectionId()))
                    .date(a.getAttendanceDate()).session(a.getSession()).status(a.getStatus()).reason(a.getReason())
                    .markedBy(a.getMarkedBy()).followUpStatus(a.getFollowUpStatus()).followUpNote(a.getFollowUpNote())
                    .followUpBy(a.getFollowUpBy()).followUpAt(a.getFollowUpAt())
                    .build();
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<AttendanceRecordResponse> sorted(List<AttendanceRecordResponse> list) {
        list.sort(Comparator.comparing(AttendanceRecordResponse::getDate).reversed()
                .thenComparing(AttendanceRecordResponse::getSession, Comparator.reverseOrder())
                .thenComparing(r -> r.getFullName() == null ? "" : r.getFullName()));
        return list;
    }

    private List<StudentMaster> onRoll(UUID sectionId) {
        return studentRepository.findBySectionIdOrderByRollNumberAsc(sectionId).stream()
                .filter(s -> ON_ROLL.contains(s.getStudentStatus())).toList();
    }

    private Section findSection(UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "sectionId", sectionId));
    }

    /** The section's class; a section of another tenant is reported as not found. */
    private Grade findGrade(Section section) {
        Grade grade = gradeRepository.findById(section.getGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", section.getGradeId()));
        tenantGuard.requireTenantRecord(grade.getTenantId(), "Section", "sectionId", section.getSectionId());
        return grade;
    }

    /** The tenant named in the request (checked against the caller's), else the caller's own tenant. */
    private String resolveTenant(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            tenantGuard.requireTenant(tenantId);
            return tenantId;
        }
        return tenantGuard.currentTenantId().orElseThrow(() -> new BadRequestException("tenantId is required"));
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BadRequestException("'from' must not be after 'to'");
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) throw new BadRequestException("The date range may span at most " + MAX_RANGE_DAYS + " days");
    }

    private static int count(List<StudentAttendance> rows, AttendanceStatus status) {
        return (int) rows.stream().filter(a -> a.getStatus() == status).count();
    }

    private static BigDecimal rate(int part, int whole) {
        return whole == 0 ? null : BigDecimal.valueOf(part * 100.0 / whole).setScale(1, RoundingMode.HALF_UP);
    }
}
