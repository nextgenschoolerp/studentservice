package com.kvs.student.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_activity_credits", indexes = {
        @Index(name = "idx_activity_student", columnList = "student_id"),
        @Index(name = "idx_activity_house", columnList = "house_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentActivityCredit extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credit_id")
    private UUID creditId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "house_id")
    private UUID houseId;

    @Column(name = "activity_name", nullable = false, length = 150)
    private String activityName;

    @Column(name = "activity_category", nullable = false, length = 50)
    private String activityCategory;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "awarded_by_user_id", length = 100)
    private String awardedByUserId;
}
