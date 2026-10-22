package com.kvs.student.repo;

import com.kvs.student.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {

    List<AcademicTerm> findByTenantIdOrderByStartDateAsc(String tenantId);

    List<AcademicTerm> findByAcademicYearIdOrderBySortOrderAsc(UUID academicYearId);

    void deleteByAcademicYearId(UUID academicYearId);
}
