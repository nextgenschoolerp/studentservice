package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "student_guardians", indexes = {
        @Index(name = "idx_guardians_student", columnList = "student_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGuardian extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "guardian_id")
    private UUID guardianId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "is_primary_contact")
    @Builder.Default
    private Boolean isPrimaryContact = true;
}
