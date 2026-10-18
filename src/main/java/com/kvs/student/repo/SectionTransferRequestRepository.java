package com.kvs.student.repo;

import com.kvs.student.entity.SectionTransferRequest;
import com.kvs.student.enums.TransferRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionTransferRequestRepository extends JpaRepository<SectionTransferRequest, UUID> {
    List<SectionTransferRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<SectionTransferRequest> findByStatus(TransferRequestStatus status);
}
