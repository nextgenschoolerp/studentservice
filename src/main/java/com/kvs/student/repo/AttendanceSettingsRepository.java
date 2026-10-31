package com.kvs.student.repo;

import com.kvs.student.entity.AttendanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceSettingsRepository extends JpaRepository<AttendanceSettings, String> {
}
