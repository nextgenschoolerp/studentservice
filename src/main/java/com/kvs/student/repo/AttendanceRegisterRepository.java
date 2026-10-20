package com.kvs.student.repo;

import com.kvs.student.entity.AttendanceRegister;
import com.kvs.student.enums.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRegisterRepository extends JpaRepository<AttendanceRegister, UUID> {
    Optional<AttendanceRegister> findBySectionIdAndRegisterDateAndSession(UUID sectionId, LocalDate date, AttendanceSession session);
    List<AttendanceRegister> findBySectionIdAndRegisterDate(UUID sectionId, LocalDate date);
    List<AttendanceRegister> findByTenantIdAndRegisterDateBetween(String tenantId, LocalDate from, LocalDate to);
}
