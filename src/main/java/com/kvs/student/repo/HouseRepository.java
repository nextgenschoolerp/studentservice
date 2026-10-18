package com.kvs.student.repo;

import com.kvs.student.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseRepository extends JpaRepository<House, UUID> {
    List<House> findByTenantId(String tenantId);
    Optional<House> findByTenantIdAndHouseName(String tenantId, String houseName);
}
