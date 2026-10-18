package com.kvs.student.repo;

import com.kvs.student.entity.StudentElectiveSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentElectiveSelectionRepository extends JpaRepository<StudentElectiveSelection, UUID> {
    List<StudentElectiveSelection> findByStudentId(UUID studentId);
    Optional<StudentElectiveSelection> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
}
