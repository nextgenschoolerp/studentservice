package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.*;
import com.kvs.student.entity.*;
import com.kvs.student.enums.HoldType;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.enums.TransferRequestStatus;
import com.kvs.student.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudentService {

    @Autowired
    private StudentMasterRepository studentRepository;

    @Autowired
    private StudentGuardianRepository guardianRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private AdministrativeHoldRepository holdRepository;

    @Autowired
    private StudentHealthMetricRepository healthMetricRepository;

    @Autowired
    private StudentElectiveSelectionRepository electiveRepository;

    @Autowired
    private SectionTransferRequestRepository sectionTransferRepository;

    @Autowired
    private StudentAuditService auditService;

    @Autowired
    private TenantAccessGuard tenantGuard;

    /**
     * US-03000: Generate a permanent non-reusable Student UID upon enrollment confirmation
     */
    @Transactional
    public StudentResponse enrollStudent(EnrollStudentRequest request) {
        log.info("Enrolling student: {} {} for tenant: {}", request.getFirstName(), request.getLastName(), request.getTenantId());
        tenantGuard.requireTenant(request.getTenantId());
        requirePlacement(request.getTenantId(), request.getGradeId(), request.getSectionId(), request.getHouseId());

        String studentUid = UidGenerator.generateStudentUid();
        while (studentRepository.existsByStudentUid(studentUid)) {
            studentUid = UidGenerator.generateStudentUid();
        }

        String barcode = "BC-" + studentUid;
        String qrCode = "https://verify.kvs.local/students/" + studentUid;

        StudentMaster student = StudentMaster.builder()
                .tenantId(request.getTenantId())
                .branchId(request.getBranchId())
                .studentUid(studentUid)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .medicalAlerts(request.getMedicalAlerts())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .gradeId(request.getGradeId())
                .sectionId(request.getSectionId())
                .houseId(request.getHouseId())
                .rollNumber(request.getRollNumber())
                .studentStatus(StudentStatus.ACTIVE)
                .idCardBarcode(barcode)
                .idCardQrCode(qrCode)
                .build();

        StudentMaster saved = studentRepository.save(student);

        if (request.getGuardianName() != null && !request.getGuardianName().isBlank()) {
            StudentGuardian guardian = StudentGuardian.builder()
                    .studentId(saved.getStudentId())
                    .fullName(request.getGuardianName())
                    .relationship(request.getGuardianRelationship() != null ? request.getGuardianRelationship() : "Guardian")
                    .mobileNumber(request.getGuardianPhone() != null ? request.getGuardianPhone() : request.getEmergencyContactPhone())
                    .email(request.getGuardianEmail())
                    .isPrimaryContact(true)
                    .build();
            guardianRepository.save(guardian);
        }

        auditService.logSuccess("student:" + saved.getStudentId(), "ENROLL_STUDENT",
                "Generated permanent UID " + studentUid + " for student " + saved.getFirstName() + " " + saved.getLastName());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID studentId) {
        StudentMaster student = findStudent(studentId);
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByUid(String studentUid) {
        StudentMaster student = studentRepository.findByStudentUid(studentUid)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentUid", studentUid));
        tenantGuard.requireTenantRecord(student.getTenantId(), "StudentMaster", "studentUid", studentUid);
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(String tenantId, String branchId) {
        tenantGuard.requireTenant(tenantId);
        return studentRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * US-03001: Update student medical alerts, blood groups, and emergency contacts in real time
     */
    @Transactional
    public StudentResponse updateMedicalAlerts(UUID studentId, UpdateMedicalAlertsRequest request) {
        log.info("Updating medical alerts for student: {}", studentId);
        StudentMaster student = findStudent(studentId);

        student.setBloodGroup(request.getBloodGroup());
        student.setMedicalAlerts(request.getMedicalAlerts());
        student.setEmergencyContactPhone(request.getEmergencyContactPhone());
        student.setAllergies(blankToNull(request.getAllergies()));

        StudentMaster saved = studentRepository.save(student);
        auditService.logSuccess("student:" + studentId, "UPDATE_MEDICAL_ALERTS",
                "Updated blood group to " + request.getBloodGroup() + " and emergency contact to " + request.getEmergencyContactPhone());

        return mapToResponse(saved);
    }

    /**
     * Personal and contact details of the student: name, date of birth, gender, admission details, nationality, address,
     * medication and GP contact, and collection permission. Blank optional fields are cleared.
     */
    @Transactional
    public StudentResponse updateProfile(UUID studentId, UpdateStudentProfileRequest request) {
        log.info("Updating profile details for student: {}", studentId);
        StudentMaster student = findStudent(studentId);

        student.setFirstName(request.getFirstName().trim());
        student.setLastName(request.getLastName().trim());
        student.setPreferredName(blankToNull(request.getPreferredName()));
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setNationality(blankToNull(request.getNationality()));
        student.setAdmissionDate(request.getAdmissionDate());
        student.setAdmissionNumber(blankToNull(request.getAdmissionNumber()));
        student.setResidentialAddress(blankToNull(request.getResidentialAddress()));
        student.setMedicationInstructions(blankToNull(request.getMedicationInstructions()));
        student.setGpContact(blankToNull(request.getGpContact()));
        student.setCollectionPermission(blankToNull(request.getCollectionPermission()));

        StudentMaster saved = studentRepository.save(student);
        auditService.logSuccess("student:" + studentId, "UPDATE_STUDENT_PROFILE",
                "Updated profile details for " + saved.getFirstName() + " " + saved.getLastName());
        return mapToResponse(saved);
    }

    /**
     * Replaces the student's guardians with the list sent: guardians listed with a guardianId are updated in place,
     * new ones are added, and guardians not in the list are removed. When no guardian is marked primary, the first becomes primary.
     */
    @Transactional
    public StudentResponse updateGuardians(UUID studentId, UpdateGuardiansRequest request) {
        log.info("Updating guardians for student: {}", studentId);
        StudentMaster student = findStudent(studentId);

        List<StudentGuardian> existing = guardianRepository.findByStudentId(studentId);
        Map<UUID, StudentGuardian> byId = new HashMap<>();
        existing.forEach(g -> byId.put(g.getGuardianId(), g));

        List<UpdateGuardiansRequest.Item> items = request.getGuardians();
        boolean anyPrimary = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getPrimaryContact()));
        List<UUID> kept = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            UpdateGuardiansRequest.Item item = items.get(i);
            StudentGuardian guardian = item.getGuardianId() != null ? byId.get(item.getGuardianId()) : null;
            if (guardian == null) {
                guardian = StudentGuardian.builder().studentId(studentId).build();
            }
            guardian.setFullName(item.getFullName().trim());
            guardian.setRelationship(item.getRelationship().trim());
            guardian.setMobileNumber(item.getMobileNumber() == null ? "" : item.getMobileNumber().trim());
            guardian.setEmail(blankToNull(item.getEmail()));
            guardian.setIsPrimaryContact(anyPrimary ? Boolean.TRUE.equals(item.getPrimaryContact()) : i == 0);
            kept.add(guardianRepository.save(guardian).getGuardianId());
        }

        List<StudentGuardian> removed = existing.stream().filter(g -> !kept.contains(g.getGuardianId())).toList();
        guardianRepository.deleteAll(removed);

        auditService.logSuccess("student:" + studentId, "UPDATE_GUARDIANS",
                "Saved " + kept.size() + " guardian(s) for " + student.getFirstName() + " " + student.getLastName());
        return mapToResponse(student);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * US-03002: Link multi-sibling profiles under a single consolidated parent user account
     */
    @Transactional
    public StudentResponse linkSibling(UUID studentId, LinkSiblingRequest request) {
        log.info("Linking student {} to parent account {}", studentId, request.getParentAccountId());
        StudentMaster student = findStudent(studentId);

        if (request.getSiblingStudentId() != null) {
            StudentMaster sibling = studentRepository.findByStudentId(request.getSiblingStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "siblingStudentId", request.getSiblingStudentId()));
            tenantGuard.requireTenantRecord(sibling.getTenantId(), "StudentMaster", "siblingStudentId", request.getSiblingStudentId());
            if (!student.getTenantId().equals(sibling.getTenantId())) {
                throw new BadRequestException("Siblings must belong to the same tenant institution");
            }
        }

        student.setParentAccountId(request.getParentAccountId());
        StudentMaster saved = studentRepository.save(student);

        auditService.logSuccess("student:" + studentId, "LINK_SIBLING_ACCOUNT",
                "Linked student profile to parent account " + request.getParentAccountId());

        return mapToResponse(saved);
    }

    /**
     * US-03003: Assign enrolled students to specific academic sections and school houses
     */
    @Transactional
    public StudentResponse assignSectionAndHouse(UUID studentId, AssignSectionHouseRequest request) {
        log.info("Assigning student {} to grade {}, section {}, house {}", studentId, request.getGradeId(), request.getSectionId(), request.getHouseId());
        StudentMaster student = findStudent(studentId);

        requirePlacement(student.getTenantId(), request.getGradeId(), request.getSectionId(), request.getHouseId());
        Grade grade = gradeRepository.findById(request.getGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", request.getGradeId()));
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", "sectionId", request.getSectionId()));

        if (request.getHouseId() != null) {
            student.setHouseId(request.getHouseId());
        }

        student.setGradeId(request.getGradeId());
        student.setSectionId(request.getSectionId());
        if (request.getRollNumber() != null) {
            student.setRollNumber(request.getRollNumber());
        }

        StudentMaster saved = studentRepository.save(student);
        auditService.logSuccess("student:" + studentId, "ASSIGN_SECTION_HOUSE",
                "Assigned to Grade " + grade.getGradeName() + ", Section " + section.getSectionName() + ", Roll No: " + student.getRollNumber());

        return mapToResponse(saved);
    }

    /**
     * US-03004: Design and print barcoded student id cards with embedded qr verification codes
     */
    @Transactional
    public IdCardResponse generateIdCard(UUID studentId) {
        log.info("Generating ID card for student: {}", studentId);
        StudentMaster student = findStudent(studentId);

        String barcode = student.getIdCardBarcode();
        if (barcode == null || barcode.isBlank()) {
            barcode = "BC-" + student.getStudentUid();
            student.setIdCardBarcode(barcode);
        }

        String qr = student.getIdCardQrCode();
        if (qr == null || qr.isBlank()) {
            qr = "https://verify.kvs.local/students/" + student.getStudentUid();
            student.setIdCardQrCode(qr);
        }

        studentRepository.save(student);

        String gradeName = student.getGradeId() != null
                ? gradeRepository.findById(student.getGradeId()).map(Grade::getGradeName).orElse("N/A")
                : "N/A";
        String sectionName = student.getSectionId() != null
                ? sectionRepository.findById(student.getSectionId()).map(Section::getSectionName).orElse("N/A")
                : "N/A";
        String houseName = student.getHouseId() != null
                ? houseRepository.findById(student.getHouseId()).map(House::getHouseName).orElse("N/A")
                : "N/A";

        auditService.logSuccess("student:" + studentId, "GENERATE_ID_CARD",
                "Issued barcoded ID card with verification QR for " + student.getStudentUid());

        return IdCardResponse.builder()
                .studentId(student.getStudentId())
                .studentUid(student.getStudentUid())
                .fullName(student.getFirstName() + " " + student.getLastName())
                .gradeName(gradeName)
                .sectionName(sectionName)
                .houseName(houseName)
                .barcodeData(barcode)
                .qrCodeData(qr)
                .verificationUrl(qr)
                .build();
    }

    /**
     * US-03005: Execute annual batch promotions to advance eligible students to the next grade
     */
    @Transactional
    public BatchPromotionResponse batchPromoteStudents(BatchPromotionRequest request) {
        log.info("Executing batch promotion from grade {} to grade {}", request.getFromGradeId(), request.getToGradeId());

        Grade fromGrade = gradeRepository.findById(request.getFromGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "fromGradeId", request.getFromGradeId()));
        Grade toGrade = gradeRepository.findById(request.getToGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "toGradeId", request.getToGradeId()));
        tenantGuard.requireTenantRecord(fromGrade.getTenantId(), "Grade", "fromGradeId", fromGrade.getGradeId());
        if (!fromGrade.getTenantId().equals(toGrade.getTenantId())) {
            throw new ResourceNotFoundException("Grade", "toGradeId", request.getToGradeId());
        }
        if (fromGrade.getGradeId().equals(toGrade.getGradeId())) {
            throw new BadRequestException("toGradeId must differ from fromGradeId");
        }

        // Only current students of the class move up; transferred or graduated students stay where they are
        List<StudentMaster> studentsToPromote = (request.getStudentIds() != null && !request.getStudentIds().isEmpty()
                ? studentRepository.findAllById(request.getStudentIds()).stream()
                : studentRepository.findAll().stream())
                .filter(s -> fromGrade.getGradeId().equals(s.getGradeId()))
                .filter(s -> fromGrade.getTenantId().equals(s.getTenantId()))
                .filter(s -> s.getStudentStatus() == StudentStatus.ACTIVE || s.getStudentStatus() == StudentStatus.PROMOTED)
                .collect(Collectors.toList());

        // Each student keeps their section letter in the next class (7-A → 8-A) when that section exists
        Map<UUID, UUID> nextSection = new HashMap<>();
        for (StudentMaster student : studentsToPromote) {
            UUID sectionId = student.getSectionId() == null ? null
                    : nextSection.computeIfAbsent(student.getSectionId(), id -> sectionRepository.findById(id)
                            .flatMap(sec -> sectionRepository.findByGradeIdAndSectionName(toGrade.getGradeId(), sec.getSectionName()))
                            .map(Section::getSectionId)
                            .orElse(null));
            student.setGradeId(toGrade.getGradeId());
            student.setSectionId(sectionId);
            student.setStudentStatus(StudentStatus.PROMOTED);
            studentRepository.save(student);
        }

        auditService.logSuccess("batch_promotion:" + toGrade.getGradeId(), "BATCH_PROMOTION",
                "Promoted " + studentsToPromote.size() + " students from " + fromGrade.getGradeName() + " to " + toGrade.getGradeName());

        return BatchPromotionResponse.builder()
                .nextAcademicYearId(request.getNextAcademicYearId())
                .toGradeId(toGrade.getGradeId())
                .totalPromoted(studentsToPromote.size())
                .status("SUCCESS")
                .build();
    }

    /**
     * Checks that a class, section and house named in a request belong to the student's tenant (another tenant's
     * are reported as not found) and that the section is a section of the class. Null ids are skipped.
     */
    private void requirePlacement(String tenantId, UUID gradeId, UUID sectionId, UUID houseId) {
        Grade grade = null;
        if (gradeId != null) {
            grade = gradeRepository.findById(gradeId)
                    .filter(g -> g.getTenantId().equalsIgnoreCase(tenantId))
                    .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", gradeId));
        }
        if (sectionId != null) {
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section", "sectionId", sectionId));
            Grade sectionGrade = grade != null && grade.getGradeId().equals(section.getGradeId()) ? grade
                    : gradeRepository.findById(section.getGradeId()).orElse(null);
            if (sectionGrade == null || !sectionGrade.getTenantId().equalsIgnoreCase(tenantId)) {
                throw new ResourceNotFoundException("Section", "sectionId", sectionId);
            }
            if (grade != null && !section.getGradeId().equals(grade.getGradeId())) {
                throw new BadRequestException("Section " + section.getSectionName() + " does not belong to Grade " + grade.getGradeName());
            }
        }
        if (houseId != null) {
            houseRepository.findById(houseId)
                    .filter(h -> h.getTenantId().equalsIgnoreCase(tenantId))
                    .orElseThrow(() -> new ResourceNotFoundException("House", "houseId", houseId));
        }
    }

    /** Loads a student of the caller's tenant; another tenant's student is reported as not found. */
    private StudentMaster findStudent(UUID studentId) {
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));
        tenantGuard.requireTenantRecord(student.getTenantId(), "StudentMaster", "studentId", studentId);
        return student;
    }

    /**
     * US-03010: Filter student demographic records by class, section, house, and transport route
     */
    @Transactional(readOnly = true)
    public List<StudentResponse> filterStudents(String tenantId, String branchId, UUID gradeId, UUID sectionId, UUID houseId, String transportRouteId, StudentStatus status) {
        if (tenantId != null && !tenantId.isBlank()) {
            tenantGuard.requireTenantRecord(tenantId, "Tenant", "tenantId", tenantId);
        }
        return studentRepository.findAll().stream()
                .filter(s -> tenantId == null || tenantId.isBlank() || tenantId.equalsIgnoreCase(s.getTenantId()))
                .filter(s -> tenantGuard.canAccessTenant(s.getTenantId()))
                .filter(s -> branchId == null || branchId.isBlank() || branchId.equalsIgnoreCase(s.getBranchId()))
                .filter(s -> gradeId == null || gradeId.equals(s.getGradeId()))
                .filter(s -> sectionId == null || sectionId.equals(s.getSectionId()))
                .filter(s -> houseId == null || houseId.equals(s.getHouseId()))
                .filter(s -> transportRouteId == null || transportRouteId.isBlank() || transportRouteId.equalsIgnoreCase(s.getTransportRouteId()))
                .filter(s -> status == null || status == s.getStudentStatus())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * US-03011: Bulk update student contact addresses and guardian mobile numbers
     */
    @Transactional
    public int bulkUpdateContacts(BulkContactUpdateRequest request) {
        log.info("Bulk updating contacts for {} items", request.getItems().size());
        int count = 0;
        for (BulkContactUpdateRequest.ContactUpdateItem item : request.getItems()) {
            StudentMaster student = findStudent(item.getStudentId());
            if (item.getResidentialAddress() != null) {
                student.setResidentialAddress(item.getResidentialAddress());
            }
            if (item.getEmergencyContactPhone() != null) {
                student.setEmergencyContactPhone(item.getEmergencyContactPhone());
            }
            studentRepository.save(student);

            if (item.getGuardianPhone() != null || item.getGuardianEmail() != null) {
                List<StudentGuardian> guardians = guardianRepository.findByStudentId(student.getStudentId());
                if (!guardians.isEmpty()) {
                    StudentGuardian primary = guardians.stream()
                            .filter(g -> Boolean.TRUE.equals(g.getIsPrimaryContact()))
                            .findFirst()
                            .orElse(guardians.get(0));
                    if (item.getGuardianPhone() != null) {
                        primary.setMobileNumber(item.getGuardianPhone());
                    }
                    if (item.getGuardianEmail() != null) {
                        primary.setEmail(item.getGuardianEmail());
                    }
                    guardianRepository.save(primary);
                }
            }
            count++;
        }
        auditService.logSuccess("student:bulk", "BULK_UPDATE_CONTACTS", "Updated contact details for " + count + " students");
        return count;
    }

    /**
     * US-03014: Manage student photo updates and batch export profile image directories
     */
    @Transactional
    public StudentResponse updateStudentPhoto(UUID studentId, UpdatePhotoRequest request) {
        StudentMaster student = findStudent(studentId);
        student.setProfilePhotoUrl(request.getPhotoUrl());
        StudentMaster saved = studentRepository.save(student);
        auditService.logSuccess("student:" + studentId, "UPDATE_PHOTO", "Updated profile photo URL for " + student.getStudentUid());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PhotoExportItemResponse> exportPhotoDirectory(UUID gradeId, UUID sectionId) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", gradeId));
        tenantGuard.requireTenantRecord(grade.getTenantId(), "Grade", "gradeId", gradeId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "sectionId", sectionId));

        return studentRepository.findByGradeIdAndSectionId(gradeId, sectionId).stream()
                .map(s -> PhotoExportItemResponse.builder()
                        .studentId(s.getStudentId())
                        .studentUid(s.getStudentUid())
                        .fullName(s.getFirstName() + " " + s.getLastName())
                        .gradeName(grade.getGradeName())
                        .sectionName(section.getSectionName())
                        .rollNumber(s.getRollNumber())
                        .photoUrl(s.getProfilePhotoUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * US-03015: Flag student profiles under administrative hold for fee or document default
     */
    @Transactional
    public AdministrativeHoldResponse placeHold(UUID studentId, PlaceHoldRequest request) {
        StudentMaster student = findStudent(studentId);
        String placedBy = SecurityUtils.getCurrentUserEmail().orElse("admin");

        AdministrativeHold hold = AdministrativeHold.builder()
                .studentId(studentId)
                .holdType(request.getHoldType())
                .reason(request.getReason())
                .isActive(true)
                .placedByUserId(placedBy)
                .placedAt(Instant.now())
                .build();

        AdministrativeHold saved = holdRepository.save(hold);
        student.setIsOnHold(true);
        student.setActiveHoldReason(request.getReason());
        studentRepository.save(student);

        auditService.logSuccess("student:" + studentId, "PLACE_ADMINISTRATIVE_HOLD",
                "Placed " + request.getHoldType() + " hold on " + student.getStudentUid() + " for: " + request.getReason());

        return mapHoldResponse(saved, student);
    }

    @Transactional
    public AdministrativeHoldResponse releaseHold(UUID holdId, ReleaseHoldRequest request) {
        AdministrativeHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("AdministrativeHold", "holdId", holdId));
        StudentMaster student = findStudent(hold.getStudentId());

        String releasedBy = SecurityUtils.getCurrentUserEmail().orElse("admin");
        hold.setIsActive(false);
        hold.setReleasedAt(Instant.now());
        hold.setReleasedByUserId(releasedBy);
        hold.setReleaseRemarks(request.getReleaseRemarks());
        AdministrativeHold saved = holdRepository.save(hold);

        boolean hasOtherActiveHolds = holdRepository.existsByStudentIdAndIsActiveTrue(student.getStudentId());
        if (!hasOtherActiveHolds) {
            student.setIsOnHold(false);
            student.setActiveHoldReason(null);
            studentRepository.save(student);
        }

        auditService.logSuccess("student:" + student.getStudentId(), "RELEASE_ADMINISTRATIVE_HOLD",
                "Released hold " + holdId + " on " + student.getStudentUid());

        return mapHoldResponse(saved, student);
    }

    @Transactional(readOnly = true)
    public List<AdministrativeHoldResponse> listHolds(UUID studentId) {
        StudentMaster student = findStudent(studentId);
        return holdRepository.findByStudentIdOrderByPlacedAtDesc(studentId).stream()
                .map(h -> mapHoldResponse(h, student))
                .collect(Collectors.toList());
    }

    /**
     * US-03016: Track student height and weight metrics during annual health checkups
     */
    @Transactional
    public HealthMetricResponse recordHealthMetric(UUID studentId, RecordHealthMetricRequest request) {
        StudentMaster student = findStudent(studentId);
        String checkedBy = SecurityUtils.getCurrentUserEmail().orElse("nurse");

        BigDecimal heightM = request.getHeightCm().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightM.multiply(heightM);
        BigDecimal bmi = request.getWeightKg().divide(heightSquared, 2, RoundingMode.HALF_UP);

        StudentHealthMetric metric = StudentHealthMetric.builder()
                .studentId(studentId)
                .checkupDate(request.getCheckupDate())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .bmi(bmi)
                .visionLeft(request.getVisionLeft())
                .visionRight(request.getVisionRight())
                .healthNotes(request.getHealthNotes())
                .checkedByUserId(checkedBy)
                .build();

        StudentHealthMetric saved = healthMetricRepository.save(metric);
        auditService.logSuccess("student:" + studentId, "RECORD_HEALTH_METRIC",
                "Recorded health metric for " + student.getStudentUid() + " (Height: " + request.getHeightCm() + " cm, Weight: " + request.getWeightKg() + " kg)");

        return mapHealthMetricResponse(saved, student);
    }

    @Transactional(readOnly = true)
    public List<HealthMetricResponse> listHealthMetrics(UUID studentId) {
        StudentMaster student = findStudent(studentId);
        return healthMetricRepository.findByStudentIdOrderByCheckupDateDesc(studentId).stream()
                .map(m -> mapHealthMetricResponse(m, student))
                .collect(Collectors.toList());
    }

    /**
     * US-03017: Record student language preferences and elective course selections
     */
    @Transactional
    public ElectiveSelectionResponse recordElectiveSelection(UUID studentId, RecordElectiveSelectionRequest request) {
        StudentMaster student = findStudent(studentId);
        String approvedBy = SecurityUtils.getCurrentUserEmail().orElse("clerk");

        StudentElectiveSelection selection = electiveRepository.findByStudentIdAndAcademicYearId(studentId, request.getAcademicYearId())
                .orElseGet(() -> StudentElectiveSelection.builder()
                        .studentId(studentId)
                        .academicYearId(request.getAcademicYearId())
                        .build());

        selection.setFirstLanguage(request.getFirstLanguage());
        selection.setSecondLanguage(request.getSecondLanguage());
        selection.setThirdLanguage(request.getThirdLanguage());
        selection.setElectiveSubjectCode(request.getElectiveSubjectCode());
        selection.setStatus("CONFIRMED");
        selection.setApprovedByUserId(approvedBy);

        StudentElectiveSelection saved = electiveRepository.save(selection);
        auditService.logSuccess("student:" + studentId, "RECORD_ELECTIVE_SELECTION",
                "Recorded elective choices for " + student.getStudentUid() + " (Elective: " + request.getElectiveSubjectCode() + ")");

        return mapElectiveResponse(saved, student);
    }

    @Transactional(readOnly = true)
    public List<ElectiveSelectionResponse> getElectives(UUID studentId) {
        StudentMaster student = findStudent(studentId);
        return electiveRepository.findByStudentId(studentId).stream()
                .map(e -> mapElectiveResponse(e, student))
                .collect(Collectors.toList());
    }

    /**
     * US-03018: Generate class enrollment roll lists for daily attendance verification
     */
    @Transactional(readOnly = true)
    public ClassRollListResponse getSectionRollList(UUID sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "sectionId", sectionId));
        Grade grade = gradeRepository.findById(section.getGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", section.getGradeId()));
        tenantGuard.requireTenantRecord(grade.getTenantId(), "Grade", "gradeId", grade.getGradeId());

        List<StudentMaster> students = studentRepository.findBySectionIdOrderByRollNumberAsc(sectionId).stream()
                .filter(s -> s.getStudentStatus() == StudentStatus.ACTIVE || s.getStudentStatus() == StudentStatus.PROMOTED)
                .collect(Collectors.toList());

        List<ClassRollListResponse.RollListStudentItem> items = students.stream()
                .map(s -> {
                    String houseName = s.getHouseId() != null
                            ? houseRepository.findById(s.getHouseId()).map(House::getHouseName).orElse("N/A")
                            : "N/A";
                    return ClassRollListResponse.RollListStudentItem.builder()
                            .studentId(s.getStudentId())
                            .studentUid(s.getStudentUid())
                            .rollNumber(s.getRollNumber())
                            .fullName(s.getFirstName() + " " + s.getLastName())
                            .gender(s.getGender() != null ? s.getGender().name() : null)
                            .bloodGroup(s.getBloodGroup())
                            .houseName(houseName)
                            .emergencyContactPhone(s.getEmergencyContactPhone())
                            .isOnHold(Boolean.TRUE.equals(s.getIsOnHold()))
                            .build();
                })
                .collect(Collectors.toList());

        return ClassRollListResponse.builder()
                .gradeId(grade.getGradeId())
                .gradeName(grade.getGradeName())
                .sectionId(section.getSectionId())
                .sectionName(section.getSectionName())
                .generatedDate(LocalDate.now())
                .totalActiveStudents(items.size())
                .students(items)
                .build();
    }

    /**
     * US-03019: Process student section transfer requests with academic head approval
     */
    @Transactional
    public SectionTransferResponse proposeSectionTransfer(UUID studentId, SectionTransferProposalRequest request) {
        StudentMaster student = findStudent(studentId);
        Section toSection = sectionRepository.findById(request.getTargetSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", "targetSectionId", request.getTargetSectionId()));

        if (student.getSectionId() == null) {
            throw new BadRequestException("Student has no current section assigned");
        }
        if (student.getSectionId().equals(request.getTargetSectionId())) {
            throw new BadRequestException("Target section cannot be the student's current section");
        }

        String requestedBy = SecurityUtils.getCurrentUserEmail().orElse("staff");
        SectionTransferRequest transfer = SectionTransferRequest.builder()
                .studentId(studentId)
                .fromSectionId(student.getSectionId())
                .toSectionId(request.getTargetSectionId())
                .reason(request.getReason())
                .status(TransferRequestStatus.PENDING)
                .requestedByUserId(requestedBy)
                .build();

        SectionTransferRequest saved = sectionTransferRepository.save(transfer);
        auditService.logSuccess("student:" + studentId, "PROPOSE_SECTION_TRANSFER",
                "Proposed section transfer for " + student.getStudentUid() + " to section " + toSection.getSectionName());

        return mapSectionTransferResponse(saved, student);
    }

    @Transactional
    public SectionTransferResponse reviewSectionTransfer(UUID transferId, ReviewSectionTransferRequest request) {
        SectionTransferRequest transfer = sectionTransferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("SectionTransferRequest", "transferId", transferId));
        StudentMaster student = findStudent(transfer.getStudentId());

        String reviewer = SecurityUtils.getCurrentUserEmail().orElse("principal");
        transfer.setStatus(request.getStatus());
        transfer.setApprovedByUserId(reviewer);
        transfer.setApprovedAt(Instant.now());
        transfer.setReviewRemarks(request.getReviewRemarks());

        if (request.getStatus() == TransferRequestStatus.APPROVED) {
            student.setSectionId(transfer.getToSectionId());
            studentRepository.save(student);
        }

        SectionTransferRequest saved = sectionTransferRepository.save(transfer);
        auditService.logSuccess("student:" + student.getStudentId(), "REVIEW_SECTION_TRANSFER",
                "Reviewed section transfer " + transferId + " with outcome " + request.getStatus());

        return mapSectionTransferResponse(saved, student);
    }

    @Transactional(readOnly = true)
    public List<SectionTransferResponse> listSectionTransfers(UUID studentId) {
        StudentMaster student = findStudent(studentId);
        return sectionTransferRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(t -> mapSectionTransferResponse(t, student))
                .collect(Collectors.toList());
    }

    private AdministrativeHoldResponse mapHoldResponse(AdministrativeHold h, StudentMaster s) {
        return AdministrativeHoldResponse.builder()
                .holdId(h.getHoldId())
                .studentId(s.getStudentId())
                .studentUid(s.getStudentUid())
                .studentName(s.getFirstName() + " " + s.getLastName())
                .holdType(h.getHoldType())
                .reason(h.getReason())
                .isActive(h.getIsActive())
                .placedByUserId(h.getPlacedByUserId())
                .placedAt(h.getPlacedAt())
                .releasedByUserId(h.getReleasedByUserId())
                .releasedAt(h.getReleasedAt())
                .releaseRemarks(h.getReleaseRemarks())
                .build();
    }

    private HealthMetricResponse mapHealthMetricResponse(StudentHealthMetric m, StudentMaster s) {
        return HealthMetricResponse.builder()
                .metricId(m.getMetricId())
                .studentId(s.getStudentId())
                .studentUid(s.getStudentUid())
                .studentName(s.getFirstName() + " " + s.getLastName())
                .checkupDate(m.getCheckupDate())
                .heightCm(m.getHeightCm())
                .weightKg(m.getWeightKg())
                .bmi(m.getBmi())
                .visionLeft(m.getVisionLeft())
                .visionRight(m.getVisionRight())
                .healthNotes(m.getHealthNotes())
                .checkedByUserId(m.getCheckedByUserId())
                .build();
    }

    private ElectiveSelectionResponse mapElectiveResponse(StudentElectiveSelection e, StudentMaster s) {
        return ElectiveSelectionResponse.builder()
                .selectionId(e.getSelectionId())
                .studentId(s.getStudentId())
                .studentUid(s.getStudentUid())
                .studentName(s.getFirstName() + " " + s.getLastName())
                .academicYearId(e.getAcademicYearId())
                .firstLanguage(e.getFirstLanguage())
                .secondLanguage(e.getSecondLanguage())
                .thirdLanguage(e.getThirdLanguage())
                .electiveSubjectCode(e.getElectiveSubjectCode())
                .status(e.getStatus())
                .approvedByUserId(e.getApprovedByUserId())
                .build();
    }

    private SectionTransferResponse mapSectionTransferResponse(SectionTransferRequest t, StudentMaster s) {
        String fromSec = sectionRepository.findById(t.getFromSectionId()).map(Section::getSectionName).orElse("N/A");
        String toSec = sectionRepository.findById(t.getToSectionId()).map(Section::getSectionName).orElse("N/A");

        return SectionTransferResponse.builder()
                .transferId(t.getTransferId())
                .studentId(s.getStudentId())
                .studentUid(s.getStudentUid())
                .studentName(s.getFirstName() + " " + s.getLastName())
                .fromSectionId(t.getFromSectionId())
                .fromSectionName(fromSec)
                .toSectionId(t.getToSectionId())
                .toSectionName(toSec)
                .reason(t.getReason())
                .status(t.getStatus())
                .requestedByUserId(t.getRequestedByUserId())
                .approvedByUserId(t.getApprovedByUserId())
                .approvedAt(t.getApprovedAt())
                .reviewRemarks(t.getReviewRemarks())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private StudentResponse mapToResponse(StudentMaster student) {
        String gradeName = student.getGradeId() != null
                ? gradeRepository.findById(student.getGradeId()).map(Grade::getGradeName).orElse(null)
                : null;
        String sectionName = student.getSectionId() != null
                ? sectionRepository.findById(student.getSectionId()).map(Section::getSectionName).orElse(null)
                : null;
        String houseName = student.getHouseId() != null
                ? houseRepository.findById(student.getHouseId()).map(House::getHouseName).orElse(null)
                : null;

        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .tenantId(student.getTenantId())
                .branchId(student.getBranchId())
                .studentUid(student.getStudentUid())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .bloodGroup(student.getBloodGroup())
                .medicalAlerts(student.getMedicalAlerts())
                .emergencyContactPhone(student.getEmergencyContactPhone())
                .gradeId(student.getGradeId())
                .gradeName(gradeName)
                .sectionId(student.getSectionId())
                .sectionName(sectionName)
                .houseId(student.getHouseId())
                .houseName(houseName)
                .rollNumber(student.getRollNumber())
                .studentStatus(student.getStudentStatus())
                .parentAccountId(student.getParentAccountId())
                .idCardBarcode(student.getIdCardBarcode())
                .idCardQrCode(student.getIdCardQrCode())
                .profilePhotoUrl(student.getProfilePhotoUrl())
                .residentialAddress(student.getResidentialAddress())
                .preferredName(student.getPreferredName())
                .nationality(student.getNationality())
                .admissionDate(student.getAdmissionDate())
                .admissionNumber(student.getAdmissionNumber())
                .allergies(student.getAllergies())
                .medicationInstructions(student.getMedicationInstructions())
                .gpContact(student.getGpContact())
                .collectionPermission(student.getCollectionPermission())
                .transportRouteId(student.getTransportRouteId())
                .isOnHold(student.getIsOnHold())
                .activeHoldReason(student.getActiveHoldReason())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .guardians(guardianRepository.findByStudentId(student.getStudentId()).stream()
                        .sorted(Comparator.comparing((StudentGuardian g) -> !Boolean.TRUE.equals(g.getIsPrimaryContact()))
                                .thenComparing(StudentGuardian::getFullName))
                        .map(g -> StudentResponse.Guardian.builder()
                                .guardianId(g.getGuardianId())
                                .fullName(g.getFullName())
                                .relationship(g.getRelationship())
                                .mobileNumber(g.getMobileNumber())
                                .email(g.getEmail())
                                .primaryContact(Boolean.TRUE.equals(g.getIsPrimaryContact()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
