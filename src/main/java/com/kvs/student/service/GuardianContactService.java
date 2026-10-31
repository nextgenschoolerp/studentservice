package com.kvs.student.service;

import com.kvs.student.entity.Grade;
import com.kvs.student.entity.StudentGuardian;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.repo.GradeRepository;
import com.kvs.student.repo.StudentGuardianRepository;
import com.kvs.student.repo.StudentMasterRepository;
import com.kvs.core.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** One contact per student on roll: the primary guardian, else the first one recorded. Students without a guardian are listed with no contact. */
@Service
public class GuardianContactService {

    public record GuardianContact(UUID studentId, String studentName, String className, String guardianName, String relationship,
                                  String phone, String email) { }

    @Autowired private StudentMasterRepository students;
    @Autowired private StudentGuardianRepository guardians;
    @Autowired private GradeRepository grades;
    @Autowired private TenantAccessGuard tenantGuard;

    @Transactional(readOnly = true)
    public List<GuardianContact> contacts(String tenantId, String gradeCode) {
        tenantGuard.requireTenant(tenantId);
        Map<UUID, Grade> gradeById = grades.findByTenantIdOrderBySequenceOrderAsc(tenantId).stream()
                .collect(Collectors.toMap(Grade::getGradeId, Function.identity()));
        UUID onlyGrade = null;
        if (gradeCode != null && !gradeCode.isBlank()) {
            onlyGrade = grades.findByTenantIdAndGradeCode(tenantId, gradeCode.trim())
                    .orElseThrow(() -> new BadRequestException("Unknown class " + gradeCode)).getGradeId();
        }
        UUID g = onlyGrade;
        List<StudentMaster> roll = students.findByTenantId(tenantId).stream()
                .filter(s -> s.getStudentStatus() == StudentStatus.ACTIVE && (g == null || g.equals(s.getGradeId())))
                .sorted(Comparator.comparing((StudentMaster s) -> Optional.ofNullable(gradeById.get(s.getGradeId())).map(Grade::getSequenceOrder).orElse(999))
                        .thenComparing(StudentMaster::getFirstName))
                .toList();
        Map<UUID, List<StudentGuardian>> byStudent = guardians.findByStudentIdIn(roll.stream().map(StudentMaster::getStudentId).toList())
                .stream().collect(Collectors.groupingBy(StudentGuardian::getStudentId));
        return roll.stream().map(s -> {
            StudentGuardian best = byStudent.getOrDefault(s.getStudentId(), List.of()).stream()
                    .min(Comparator.comparing((StudentGuardian x) -> !Boolean.TRUE.equals(x.getIsPrimaryContact()))).orElse(null);
            Grade grade = gradeById.get(s.getGradeId());
            return new GuardianContact(s.getStudentId(), s.getFirstName() + " " + s.getLastName(), grade == null ? null : grade.getGradeName(),
                    best == null ? null : best.getFullName(), best == null ? null : best.getRelationship(),
                    best == null ? null : best.getMobileNumber(), best == null ? null : best.getEmail());
        }).toList();
    }
}
