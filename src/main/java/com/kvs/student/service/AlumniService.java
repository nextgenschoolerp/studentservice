package com.kvs.student.service;

import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.student.dto.AlumniResponse;
import com.kvs.student.dto.ArchiveAlumniRequest;
import com.kvs.student.entity.AlumniRecord;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.repo.AlumniRecordRepository;
import com.kvs.student.repo.StudentMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return mapToResponse(alumni);
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
