package com.kvs.student.repo;

import com.kvs.student.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentDocumentRepository extends JpaRepository<StudentDocument, UUID> {
    List<StudentDocument> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
