package com.kvs.student.repo;

import com.kvs.student.entity.CertificateIssued;
import com.kvs.student.enums.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateIssuedRepository extends JpaRepository<CertificateIssued, UUID> {
    List<CertificateIssued> findByStudentId(UUID studentId);
    Optional<CertificateIssued> findByCertificateNumber(String certificateNumber);
    boolean existsByCertificateNumber(String certificateNumber);
    List<CertificateIssued> findByStudentIdAndCertificateType(UUID studentId, CertificateType certificateType);
}
