package com.kvs.student.repo;

import com.kvs.student.entity.DisciplinaryIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisciplinaryIncidentRepository extends JpaRepository<DisciplinaryIncident, UUID> {
    List<DisciplinaryIncident> findByStudentIdOrderByIncidentDateDesc(UUID studentId);
}
