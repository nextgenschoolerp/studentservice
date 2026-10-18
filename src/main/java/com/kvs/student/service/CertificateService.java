package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.CertificateResponse;
import com.kvs.student.dto.IssueBonafideCertificateRequest;
import com.kvs.student.dto.IssueTransferCertificateRequest;
import com.kvs.student.entity.CertificateIssued;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.CertificateType;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.repo.CertificateIssuedRepository;
import com.kvs.student.repo.StudentMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CertificateService {

    @Autowired
    private CertificateIssuedRepository certificateRepository;

    @Autowired
    private StudentMasterRepository studentRepository;

    @Autowired
    private StudentAuditService auditService;

    /**
     * US-03006: Generate official transfer certificates (TC) with dual administrative sign-off
     */
    @Transactional
    public CertificateResponse issueTransferCertificate(UUID studentId, IssueTransferCertificateRequest request) {
        log.info("Issuing Transfer Certificate for student: {}", studentId);
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        String firstAuthorizer = SecurityUtils.getCurrentUserEmail().orElse("registrar");
        if (request.getSecondSignOffUserId() == null || request.getSecondSignOffUserId().isBlank()) {
            throw new BadRequestException("Dual administrative sign-off is required: secondSignOffUserId cannot be blank");
        }
        if (firstAuthorizer.equalsIgnoreCase(request.getSecondSignOffUserId())) {
            throw new BadRequestException("Dual administrative sign-off requires two distinct authorizers");
        }

        String certNumber = UidGenerator.generateCertificateNumber("TC");
        String qrToken = "TC-" + UUID.randomUUID();

        CertificateIssued certificate = CertificateIssued.builder()
                .studentId(studentId)
                .certificateType(CertificateType.TRANSFER_CERTIFICATE)
                .certificateNumber(certNumber)
                .issueDate(LocalDate.now())
                .authorizedByUserId(firstAuthorizer)
                .secondSignOffUserId(request.getSecondSignOffUserId())
                .qrVerificationToken(qrToken)
                .pdfVaultPath("/vault/certificates/" + certNumber + ".pdf")
                .remarks("Reason: " + request.getReasonForLeaving() + " | Conduct: " + request.getConductRemarks())
                .build();

        CertificateIssued saved = certificateRepository.save(certificate);

        student.setStudentStatus(StudentStatus.TRANSFERRED);
        studentRepository.save(student);

        auditService.logSuccess("certificate:" + saved.getCertificateId(), "ISSUE_TRANSFER_CERTIFICATE",
                "Issued TC " + certNumber + " with dual sign-off (" + firstAuthorizer + " & " + request.getSecondSignOffUserId() + ")");

        return mapToResponse(saved, student);
    }

    /**
     * US-03007: Issue digital bonafide and character certificates with tamper-evident QR codes
     */
    @Transactional
    public CertificateResponse issueBonafideCertificate(UUID studentId, IssueBonafideCertificateRequest request) {
        log.info("Issuing certificate {} for student: {}", request.getCertificateType(), studentId);
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        String authorizer = SecurityUtils.getCurrentUserEmail().orElse("registrar");
        String prefix = request.getCertificateType() == CertificateType.CHARACTER ? "CHAR" : "BON";
        String certNumber = UidGenerator.generateCertificateNumber(prefix);
        String qrToken = prefix + "-" + UUID.randomUUID();

        CertificateIssued certificate = CertificateIssued.builder()
                .studentId(studentId)
                .certificateType(request.getCertificateType())
                .certificateNumber(certNumber)
                .issueDate(LocalDate.now())
                .authorizedByUserId(authorizer)
                .qrVerificationToken(qrToken)
                .pdfVaultPath("/vault/certificates/" + certNumber + ".pdf")
                .remarks(request.getPurpose() != null ? request.getPurpose() : "Issued for educational purposes")
                .build();

        CertificateIssued saved = certificateRepository.save(certificate);

        auditService.logSuccess("certificate:" + saved.getCertificateId(), "ISSUE_BONAFIDE_CERTIFICATE",
                "Issued " + request.getCertificateType() + " certificate " + certNumber);

        return mapToResponse(saved, student);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> listCertificates(UUID studentId) {
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        return certificateRepository.findByStudentId(studentId).stream()
                .map(c -> mapToResponse(c, student))
                .collect(Collectors.toList());
    }

    private CertificateResponse mapToResponse(CertificateIssued cert, StudentMaster student) {
        return CertificateResponse.builder()
                .certificateId(cert.getCertificateId())
                .studentId(cert.getStudentId())
                .studentUid(student.getStudentUid())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .certificateType(cert.getCertificateType())
                .certificateNumber(cert.getCertificateNumber())
                .issueDate(cert.getIssueDate())
                .authorizedByUserId(cert.getAuthorizedByUserId())
                .secondSignOffUserId(cert.getSecondSignOffUserId())
                .qrVerificationToken(cert.getQrVerificationToken())
                .verificationUrl("https://verify.kvs.local/certificates/" + cert.getCertificateNumber())
                .remarks(cert.getRemarks())
                .build();
    }
}
