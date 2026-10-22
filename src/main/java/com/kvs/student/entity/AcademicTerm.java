package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** A term of an academic session with its half-term break (Settings → Academic setup, Calendar → Term dates). */
@Entity
@Table(name = "academic_terms", indexes = {
        @Index(name = "idx_academic_terms_session", columnList = "academic_year_id, sort_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTerm extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "term_id")
    private UUID termId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_name", nullable = false, length = 50)
    private String termName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "half_term_start")
    private LocalDate halfTermStart;

    @Column(name = "half_term_end")
    private LocalDate halfTermEnd;

    /** The term the school is in now; one per session */
    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
