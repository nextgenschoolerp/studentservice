package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.student.enums.AttendanceRegisterMode;
import jakarta.persistence.*;
import lombok.*;

/** The school's attendance settings: one register a day, or morning and afternoon registers. One row per tenant. */
@Entity
@Table(name = "attendance_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AttendanceSettings extends BaseAuditEntity {

    @Id
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "register_mode", nullable = false, length = 10)
    private AttendanceRegisterMode mode;
}
