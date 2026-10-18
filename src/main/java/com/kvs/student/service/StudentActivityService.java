package com.kvs.student.service;

import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.ActivityCreditResponse;
import com.kvs.student.dto.LogActivityCreditRequest;
import com.kvs.student.entity.House;
import com.kvs.student.entity.StudentActivityCredit;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.repo.HouseRepository;
import com.kvs.student.repo.StudentActivityCreditRepository;
import com.kvs.student.repo.StudentMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing student extracurricular activities and house points.
 * Covers user story:
 * - US-03012: Log student extracurricular participation credits and house points
 */
@Service
@Slf4j
public class StudentActivityService {

    @Autowired
    private StudentActivityCreditRepository activityRepository;

    @Autowired
    private StudentMasterRepository studentRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private StudentAuditService auditService;

    @Transactional
    public ActivityCreditResponse logActivityCredit(UUID studentId, LogActivityCreditRequest request) {
        log.info("Logging activity credit for student: {}, activity: {}", studentId, request.getActivityName());
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        UUID houseId = request.getHouseId() != null ? request.getHouseId() : student.getHouseId();
        String houseName = "N/A";
        if (houseId != null) {
            House house = houseRepository.findById(houseId).orElse(null);
            if (house != null) {
                houseName = house.getHouseName();
            }
        }

        String awardedBy = SecurityUtils.getCurrentUserEmail().orElse("system");

        StudentActivityCredit credit = StudentActivityCredit.builder()
                .studentId(studentId)
                .houseId(houseId)
                .activityName(request.getActivityName())
                .activityCategory(request.getActivityCategory())
                .pointsAwarded(request.getPointsAwarded())
                .eventDate(request.getEventDate())
                .remarks(request.getRemarks())
                .awardedByUserId(awardedBy)
                .build();

        StudentActivityCredit saved = activityRepository.save(credit);

        auditService.logSuccess("student:" + studentId, "LOG_ACTIVITY_CREDIT",
                "Awarded " + request.getPointsAwarded() + " points for " + request.getActivityName());

        return ActivityCreditResponse.builder()
                .creditId(saved.getCreditId())
                .studentId(student.getStudentId())
                .studentUid(student.getStudentUid())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .houseId(houseId)
                .houseName(houseName)
                .activityName(saved.getActivityName())
                .activityCategory(saved.getActivityCategory())
                .pointsAwarded(saved.getPointsAwarded())
                .eventDate(saved.getEventDate())
                .remarks(saved.getRemarks())
                .awardedByUserId(saved.getAwardedByUserId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ActivityCreditResponse> listActivityCredits(UUID studentId) {
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        return activityRepository.findByStudentIdOrderByEventDateDesc(studentId).stream()
                .map(c -> {
                    String houseName = c.getHouseId() != null
                            ? houseRepository.findById(c.getHouseId()).map(House::getHouseName).orElse("N/A")
                            : "N/A";
                    return ActivityCreditResponse.builder()
                            .creditId(c.getCreditId())
                            .studentId(student.getStudentId())
                            .studentUid(student.getStudentUid())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .houseId(c.getHouseId())
                            .houseName(houseName)
                            .activityName(c.getActivityName())
                            .activityCategory(c.getActivityCategory())
                            .pointsAwarded(c.getPointsAwarded())
                            .eventDate(c.getEventDate())
                            .remarks(c.getRemarks())
                            .awardedByUserId(c.getAwardedByUserId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Integer getHouseTotalPoints(UUID houseId) {
        return activityRepository.sumPointsByHouseId(houseId);
    }
}
