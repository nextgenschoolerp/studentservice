package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "alumni_records", indexes = {
        @Index(name = "idx_alumni_student_uid", columnList = "student_uid", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniRecord extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alumni_id")
    private UUID alumniId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @Column(name = "student_uid", unique = true, nullable = false, length = 50)
    private String studentUid;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "graduation_year", nullable = false)
    private Integer graduationYear;

    @Column(name = "final_grade_passed", nullable = false, length = 50)
    private String finalGradePassed;

    @Column(name = "alumni_email", length = 150)
    private String alumniEmail;

    @Column(name = "alumni_phone", length = 20)
    private String alumniPhone;

    @Column(name = "higher_education_institution", length = 200)
    private String higherEducationInstitution;

    @Column(name = "current_occupation", length = 200)
    private String currentOccupation;

    @Column(name = "archived_date", nullable = false)
    private LocalDate archivedDate;
}
