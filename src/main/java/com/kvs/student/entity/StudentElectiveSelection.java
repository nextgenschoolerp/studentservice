package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "student_elective_selections", indexes = {
        @Index(name = "idx_elective_student_year", columnList = "student_id, academic_year_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentElectiveSelection extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "selection_id")
    private UUID selectionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "first_language", nullable = false, length = 50)
    private String firstLanguage;

    @Column(name = "second_language", nullable = false, length = 50)
    private String secondLanguage;

    @Column(name = "third_language", length = 50)
    private String thirdLanguage;

    @Column(name = "elective_subject_code", nullable = false, length = 50)
    private String electiveSubjectCode;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(name = "approved_by_user_id", length = 100)
    private String approvedByUserId;
}
