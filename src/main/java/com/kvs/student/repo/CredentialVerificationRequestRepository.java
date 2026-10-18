package com.kvs.student.repo;

import com.kvs.student.entity.CredentialVerificationRequest;
import com.kvs.student.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CredentialVerificationRequestRepository extends JpaRepository<CredentialVerificationRequest, UUID> {
    List<CredentialVerificationRequest> findByStudentUid(String studentUid);
    List<CredentialVerificationRequest> findByStatus(VerificationStatus status);
}
