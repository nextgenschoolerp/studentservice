package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "houses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class House extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "house_id")
    private UUID houseId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "house_name", nullable = false, length = 50)
    private String houseName;

    @Column(name = "color_hex", length = 7)
    private String colorHex;
}
