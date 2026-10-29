package com.kvs.student.importer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentImportJobRepository extends JpaRepository<StudentImportJob, UUID> {
    List<StudentImportJob> findTop50ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
