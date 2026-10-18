package com.kvs.student.repo;

import com.kvs.student.entity.AlumniRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlumniRecordRepository extends JpaRepository<AlumniRecord, UUID> {
    Optional<AlumniRecord> findByStudentUid(String studentUid);
    Optional<AlumniRecord> findByStudentId(UUID studentId);
    boolean existsByStudentUid(String studentUid);
}
