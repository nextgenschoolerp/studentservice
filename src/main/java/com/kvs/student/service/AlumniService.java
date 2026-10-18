package com.kvs.student.service;

import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.AlumniResponse;
import com.kvs.student.dto.ArchiveAlumniRequest;
import com.kvs.student.dto.CredentialVerificationResponse;
import com.kvs.student.dto.ReviewCredentialVerificationRequest;
import com.kvs.student.dto.SubmitCredentialVerificationRequest;
import com.kvs.student.entity.AlumniRecord;
import com.kvs.student.entity.CredentialVerificationRequest;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.enums.VerificationStatus;
import com.kvs.student.repo.AlumniRecordRepository;
import com.kvs.student.repo.CredentialVerificationRequestRepository;
import com.kvs.student.repo.StudentMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class AlumniService {

    @Autowired
    private AlumniRecordRepository alumniRepository;

    @Autowired
    private StudentMasterRepository studentRepository;

    @Autowired
    private StudentAuditService auditService;

    @Autowired
    private TenantAccessGuard tenantGuard;

    /**
     * US-03009: Archive graduating student profiles into the permanent alumni registry
     */
    @Transactional
    public AlumniResponse archiveAlumni(ArchiveAlumniRequest request) {
        log.info("Archiving graduating student: {} into alumni registry", request.getStudentId());
        StudentMaster student = studentRepository.findByStudentId(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", request.getStudentId()));

        if (alumniRepository.existsByStudentUid(student.getStudentUid())) {
            throw new ConflictException("Student " + student.getStudentUid() + " is already archived in the alumni registry");
        }

        AlumniRecord alumni = AlumniRecord.builder()
                .studentId(student.getStudentId())
                .tenantId(student.getTenantId())
                .branchId(student.getBranchId())
                .studentUid(student.getStudentUid())
                .fullName(student.getFirstName() + " " + student.getLastName())
                .graduationYear(request.getGraduationYear())
                .finalGradePassed(request.getFinalGradePassed())
                .alumniEmail(request.getAlumniEmail() != null ? request.getAlumniEmail() : student.getStudentUid() + "@alumni.kvs.local")
                .alumniPhone(request.getAlumniPhone() != null ? request.getAlumniPhone() : student.getEmergencyContactPhone())
                .higherEducationInstitution(request.getHigherEducationInstitution())
                .currentOccupation(request.getCurrentOccupation())
                .archivedDate(LocalDate.now())
                .build();

        AlumniRecord saved = alumniRepository.save(alumni);

        student.setStudentStatus(StudentStatus.GRADUATED);
        studentRepository.save(student);

        auditService.logSuccess("alumni:" + saved.getAlumniId(), "ARCHIVE_ALUMNI",
                "Archived graduating student " + student.getStudentUid() + " (Graduation Year: " + request.getGraduationYear() + ")");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AlumniResponse getAlumniByUid(String studentUid) {
        AlumniRecord alumni = alumniRepository.findByStudentUid(studentUid)
                .orElseThrow(() -> new ResourceNotFoundException("AlumniRecord", "studentUid", studentUid));
        tenantGuard.requireTenantRecord(alumni.getTenantId(), "AlumniRecord", "studentUid", studentUid);
        return mapToResponse(alumni);
    }

    @Autowired
    private CredentialVerificationRequestRepository verificationRepository;

    /**
     * US-03013: Verify former student credential requests against historical archive ledgers
     */
    @Transactional
    public CredentialVerificationResponse submitCredentialVerification(SubmitCredentialVerificationRequest request) {
        log.info("Submitting credential verification request for student UID: {}", request.getStudentUid());

        boolean existsInAlumni = alumniRepository.existsByStudentUid(request.getStudentUid());
        boolean existsInStudentMaster = studentRepository.existsByStudentUid(request.getStudentUid());

        if (!existsInAlumni && !existsInStudentMaster) {
            throw new ResourceNotFoundException("Student or Alumni", "studentUid", request.getStudentUid());
        }

        CredentialVerificationRequest entity = CredentialVerificationRequest.builder()
                .studentUid(request.getStudentUid())
                .requestorName(request.getRequestorName())
                .requestorOrganization(request.getRequestorOrganization())
                .credentialType(request.getCredentialType())
                .verificationRemarks(request.getRemarks())
                .status(VerificationStatus.PENDING)
                .build();

        CredentialVerificationRequest saved = verificationRepository.save(entity);
        auditService.logSuccess("credential_verification:" + saved.getRequestId(), "SUBMIT_CREDENTIAL_VERIFICATION",
                "Submitted verification request for " + request.getStudentUid() + " by " + request.getRequestorOrganization());

        return mapVerificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public CredentialVerificationResponse getCredentialVerification(UUID requestId) {
        CredentialVerificationRequest req = verificationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CredentialVerificationRequest", "requestId", requestId));
        return mapVerificationResponse(req);
    }

    @Transactional
    public CredentialVerificationResponse reviewCredentialVerification(UUID requestId, ReviewCredentialVerificationRequest request) {
        CredentialVerificationRequest req = verificationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CredentialVerificationRequest", "requestId", requestId));

        String reviewer = SecurityUtils.getCurrentUserEmail().orElse("alumni_coordinator");
        req.setStatus(request.getStatus());
        req.setVerificationRemarks(request.getVerificationRemarks());
        req.setVerifiedByUserId(reviewer);
        req.setVerifiedAt(Instant.now());

        CredentialVerificationRequest saved = verificationRepository.save(req);
        auditService.logSuccess("credential_verification:" + saved.getRequestId(), "REVIEW_CREDENTIAL_VERIFICATION",
                "Reviewed verification request for " + saved.getStudentUid() + " with outcome " + request.getStatus());

        return mapVerificationResponse(saved);
    }

    private CredentialVerificationResponse mapVerificationResponse(CredentialVerificationRequest req) {
        String studentName = alumniRepository.findByStudentUid(req.getStudentUid())
                .map(AlumniRecord::getFullName)
                .orElseGet(() -> studentRepository.findByStudentUid(req.getStudentUid())
                        .map(s -> s.getFirstName() + " " + s.getLastName())
                        .orElse("N/A"));

        return CredentialVerificationResponse.builder()
                .requestId(req.getRequestId())
                .studentUid(req.getStudentUid())
                .studentName(studentName)
                .requestorName(req.getRequestorName())
                .requestorOrganization(req.getRequestorOrganization())
                .credentialType(req.getCredentialType())
                .status(req.getStatus())
                .verificationRemarks(req.getVerificationRemarks())
                .verifiedByUserId(req.getVerifiedByUserId())
                .verifiedAt(req.getVerifiedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }

    private AlumniResponse mapToResponse(AlumniRecord alumni) {
        return AlumniResponse.builder()
                .alumniId(alumni.getAlumniId())
                .studentId(alumni.getStudentId())
                .studentUid(alumni.getStudentUid())
                .fullName(alumni.getFullName())
                .graduationYear(alumni.getGraduationYear())
                .finalGradePassed(alumni.getFinalGradePassed())
                .alumniEmail(alumni.getAlumniEmail())
                .alumniPhone(alumni.getAlumniPhone())
                .higherEducationInstitution(alumni.getHigherEducationInstitution())
                .currentOccupation(alumni.getCurrentOccupation())
                .archivedDate(alumni.getArchivedDate())
                .build();
    }
}
