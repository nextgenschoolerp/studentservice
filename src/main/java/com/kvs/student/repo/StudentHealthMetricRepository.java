package com.kvs.student.repo;

import com.kvs.student.entity.StudentHealthMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentHealthMetricRepository extends JpaRepository<StudentHealthMetric, UUID> {
    List<StudentHealthMetric> findByStudentIdOrderByCheckupDateDesc(UUID studentId);
}
