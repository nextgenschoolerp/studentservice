package com.kvs.student.repo;

import com.kvs.student.entity.StudentAttendance;
import com.kvs.student.enums.AttendanceSession;
import com.kvs.student.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {
    List<StudentAttendance> findBySectionIdAndAttendanceDate(UUID sectionId, LocalDate date);
    List<StudentAttendance> findByStudentIdInAndAttendanceDate(Collection<UUID> studentIds, LocalDate date);
    List<StudentAttendance> findByTenantIdAndAttendanceDateBetween(String tenantId, LocalDate from, LocalDate to);
    List<StudentAttendance> findByTenantIdAndSessionAndAttendanceDateBetween(String tenantId, AttendanceSession session, LocalDate from, LocalDate to);
    List<StudentAttendance> findByTenantIdAndStatusAndAttendanceDateBetween(String tenantId, AttendanceStatus status, LocalDate from, LocalDate to);
    List<StudentAttendance> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDescSessionDesc(UUID studentId, LocalDate from, LocalDate to);
}
