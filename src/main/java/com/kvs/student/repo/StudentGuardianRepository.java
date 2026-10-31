package com.kvs.student.repo;

import com.kvs.student.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {
    List<StudentGuardian> findByStudentId(UUID studentId);
    List<StudentGuardian> findByStudentIdIn(java.util.Collection<UUID> studentIds);
    List<StudentGuardian> findByEmail(String email);
    List<StudentGuardian> findByMobileNumber(String mobileNumber);
}
