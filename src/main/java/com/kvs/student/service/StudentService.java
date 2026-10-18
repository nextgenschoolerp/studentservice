package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.student.dto.*;
import com.kvs.student.entity.*;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private StudentAuditService auditService;

    /**
     * US-03000: Generate a permanent non-reusable Student UID upon enrollment confirmation
     */
    @Transactional
    public StudentResponse enrollStudent(EnrollStudentRequest request) {
        log.info("Enrolling student: {} {} for tenant: {}", request.getFirstName(), request.getLastName(), request.getTenantId());

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
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByUid(String studentUid) {
        StudentMaster student = studentRepository.findByStudentUid(studentUid)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentUid", studentUid));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(String tenantId, String branchId) {
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
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        student.setBloodGroup(request.getBloodGroup());
        student.setMedicalAlerts(request.getMedicalAlerts());
        student.setEmergencyContactPhone(request.getEmergencyContactPhone());

        StudentMaster saved = studentRepository.save(student);
        auditService.logSuccess("student:" + studentId, "UPDATE_MEDICAL_ALERTS",
                "Updated blood group to " + request.getBloodGroup() + " and emergency contact to " + request.getEmergencyContactPhone());

        return mapToResponse(saved);
    }

    /**
     * US-03002: Link multi-sibling profiles under a single consolidated parent user account
     */
    @Transactional
    public StudentResponse linkSibling(UUID studentId, LinkSiblingRequest request) {
        log.info("Linking student {} to parent account {}", studentId, request.getParentAccountId());
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        if (request.getSiblingStudentId() != null) {
            StudentMaster sibling = studentRepository.findByStudentId(request.getSiblingStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "siblingStudentId", request.getSiblingStudentId()));
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
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        Grade grade = gradeRepository.findById(request.getGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", request.getGradeId()));

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", "sectionId", request.getSectionId()));

        if (!section.getGradeId().equals(grade.getGradeId())) {
            throw new BadRequestException("Section " + section.getSectionName() + " does not belong to Grade " + grade.getGradeName());
        }

        if (request.getHouseId() != null) {
            houseRepository.findById(request.getHouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("House", "houseId", request.getHouseId()));
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
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

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

        List<StudentMaster> studentsToPromote;
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            studentsToPromote = studentRepository.findAllById(request.getStudentIds()).stream()
                    .filter(s -> s.getGradeId().equals(fromGrade.getGradeId()))
                    .collect(Collectors.toList());
        } else {
            studentsToPromote = studentRepository.findAll().stream()
                    .filter(s -> fromGrade.getGradeId().equals(s.getGradeId()))
                    .filter(s -> s.getStudentStatus() == StudentStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        for (StudentMaster student : studentsToPromote) {
            student.setGradeId(toGrade.getGradeId());
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
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
