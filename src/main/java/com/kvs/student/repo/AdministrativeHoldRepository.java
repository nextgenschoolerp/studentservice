package com.kvs.student.repo;

import com.kvs.student.entity.AdministrativeHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdministrativeHoldRepository extends JpaRepository<AdministrativeHold, UUID> {
    List<AdministrativeHold> findByStudentIdOrderByPlacedAtDesc(UUID studentId);
    List<AdministrativeHold> findByStudentIdAndIsActiveTrue(UUID studentId);
    boolean existsByStudentIdAndIsActiveTrue(UUID studentId);
}
