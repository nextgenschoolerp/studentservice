package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.student.dto.CreateAcademicSessionRequest;
import com.kvs.student.dto.CreateGradeRequest;
import com.kvs.student.dto.CreateHouseRequest;
import com.kvs.student.dto.CreateSectionRequest;
import com.kvs.student.entity.AcademicSession;
import com.kvs.student.entity.Grade;
import com.kvs.student.entity.House;
import com.kvs.student.entity.Section;
import com.kvs.student.repo.AcademicSessionRepository;
import com.kvs.student.repo.GradeRepository;
import com.kvs.student.repo.HouseRepository;
import com.kvs.student.repo.SectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AcademicHierarchyService {

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private StudentAuditService auditService;

    @Transactional
    public AcademicSession createSession(CreateAcademicSessionRequest request) {
        if (sessionRepository.existsByTenantIdAndSessionName(request.getTenantId(), request.getSessionName())) {
            throw new ConflictException("Academic session " + request.getSessionName() + " already exists");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Session start date must precede end date");
        }

        AcademicSession session = AcademicSession.builder()
                .tenantId(request.getTenantId())
                .sessionName(request.getSessionName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        AcademicSession saved = sessionRepository.save(session);
        auditService.logSuccess("session:" + saved.getAcademicYearId(), "CREATE_SESSION", "Created academic session " + saved.getSessionName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AcademicSession> listSessions(String tenantId) {
        return sessionRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Grade createGrade(CreateGradeRequest request) {
        if (gradeRepository.existsByTenantIdAndGradeCode(request.getTenantId(), request.getGradeCode())) {
            throw new ConflictException("Grade code " + request.getGradeCode() + " already exists");
        }

        Grade grade = Grade.builder()
                .tenantId(request.getTenantId())
                .gradeName(request.getGradeName())
                .gradeCode(request.getGradeCode())
                .sequenceOrder(request.getSequenceOrder())
                .build();

        Grade saved = gradeRepository.save(grade);
        auditService.logSuccess("grade:" + saved.getGradeId(), "CREATE_GRADE", "Created grade " + saved.getGradeName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Grade> listGrades(String tenantId) {
        return gradeRepository.findByTenantIdOrderBySequenceOrderAsc(tenantId);
    }

    @Transactional
    public Section createSection(CreateSectionRequest request) {
        Section section = Section.builder()
                .gradeId(request.getGradeId())
                .sectionName(request.getSectionName())
                .maxCapacity(request.getMaxCapacity() != null ? request.getMaxCapacity() : 40)
                .build();

        Section saved = sectionRepository.save(section);
        auditService.logSuccess("section:" + saved.getSectionId(), "CREATE_SECTION", "Created section " + saved.getSectionName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Section> listSections(UUID gradeId) {
        return sectionRepository.findByGradeId(gradeId);
    }

    @Transactional
    public House createHouse(CreateHouseRequest request) {
        House house = House.builder()
                .tenantId(request.getTenantId())
                .houseName(request.getHouseName())
                .colorHex(request.getColorHex())
                .build();

        House saved = houseRepository.save(house);
        auditService.logSuccess("house:" + saved.getHouseId(), "CREATE_HOUSE", "Created house " + saved.getHouseName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<House> listHouses(String tenantId) {
        return houseRepository.findByTenantId(tenantId);
    }
}
