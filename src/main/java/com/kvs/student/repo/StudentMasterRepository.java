package com.kvs.student.repo;

import com.kvs.student.entity.StudentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentMasterRepository extends JpaRepository<StudentMaster, UUID>, JpaSpecificationExecutor<StudentMaster> {
    Optional<StudentMaster> findByStudentId(UUID studentId);
    Optional<StudentMaster> findByStudentUid(String studentUid);
    boolean existsByStudentUid(String studentUid);
    List<StudentMaster> findByTenantIdAndBranchId(String tenantId, String branchId);
    List<StudentMaster> findByGradeIdAndSectionId(UUID gradeId, UUID sectionId);
    List<StudentMaster> findBySectionIdOrderByRollNumberAsc(UUID sectionId);
    List<StudentMaster> findByParentAccountId(String parentAccountId);
    List<StudentMaster> findByTenantId(String tenantId);
    long countByGradeIdAndSectionId(UUID gradeId, UUID sectionId);
}
