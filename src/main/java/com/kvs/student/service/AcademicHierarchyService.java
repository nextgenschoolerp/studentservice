package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.student.dto.AssignClassTeacherRequest;
import com.kvs.student.dto.CreateAcademicSessionRequest;
import com.kvs.student.dto.CreateGradeRequest;
import com.kvs.student.dto.CreateHouseRequest;
import com.kvs.student.dto.CreateSectionRequest;
import com.kvs.student.dto.UpdateHouseRequest;
import com.kvs.student.dto.UpdateSectionRequest;
import com.kvs.student.entity.AcademicSession;
import com.kvs.student.entity.Grade;
import com.kvs.student.entity.House;
import com.kvs.student.entity.Section;
import com.kvs.student.repo.AcademicSessionRepository;
import com.kvs.student.repo.GradeRepository;
import com.kvs.student.repo.HouseRepository;
import com.kvs.student.repo.SectionRepository;
import com.kvs.student.repo.StudentMasterRepository;
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
    private StudentMasterRepository studentRepository;

    @Autowired
    private StructureUsageChecker usageChecker;

    @Autowired
    private StudentAuditService auditService;

    @Autowired
    private TenantAccessGuard tenantGuard;

    @Transactional
    public AcademicSession createSession(CreateAcademicSessionRequest request) {
        tenantGuard.requireTenant(request.getTenantId());
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
        tenantGuard.requireTenant(tenantId);
        return sessionRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Grade createGrade(CreateGradeRequest request) {
        tenantGuard.requireTenant(request.getTenantId());
        if (gradeRepository.existsByTenantIdAndGradeCode(request.getTenantId(), request.getGradeCode())) {
            throw new ConflictException("Grade code " + request.getGradeCode() + " already exists");
        }
        String gradeName = request.getGradeName().trim();
        if (gradeRepository.findByTenantIdOrderBySequenceOrderAsc(request.getTenantId()).stream().anyMatch(g -> g.getGradeName().equalsIgnoreCase(gradeName))) {
            throw new ConflictException("A class named " + gradeName + " already exists");
        }

        Grade grade = Grade.builder()
                .tenantId(request.getTenantId())
                .gradeName(gradeName)
                .gradeCode(request.getGradeCode().trim().toUpperCase())
                .sequenceOrder(request.getSequenceOrder())
                .build();

        Grade saved = gradeRepository.save(grade);
        auditService.logSuccess("grade:" + saved.getGradeId(), "CREATE_GRADE", "Created grade " + saved.getGradeName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Grade> listGrades(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        return gradeRepository.findByTenantIdOrderBySequenceOrderAsc(tenantId);
    }

    @Transactional
    public Section createSection(CreateSectionRequest request) {
        Grade grade = findGrade(request.getGradeId());
        String sectionName = request.getSectionName().trim().toUpperCase();
        if (sectionRepository.findByGradeId(request.getGradeId()).stream().anyMatch(s -> s.getSectionName().equalsIgnoreCase(sectionName))) {
            throw new ConflictException(grade.getGradeName() + " already has a section " + sectionName);
        }
        Section section = Section.builder()
                .gradeId(request.getGradeId())
                .sectionName(sectionName)
                .maxCapacity(request.getMaxCapacity() != null ? request.getMaxCapacity() : 40)
                .build();

        Section saved = sectionRepository.save(section);
        auditService.logSuccess("section:" + saved.getSectionId(), "CREATE_SECTION", "Created section " + saved.getSectionName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Section> listSections(UUID gradeId) {
        findGrade(gradeId);
        return sectionRepository.findByGradeId(gradeId);
    }

    /** New seat capacity of a section; it cannot be set below the students already in it. */
    @Transactional
    public Section updateSection(UUID sectionId, UpdateSectionRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        Grade grade = findGrade(section.getGradeId());
        long onRoll = studentRepository.countByGradeIdAndSectionId(section.getGradeId(), sectionId);
        if (request.getMaxCapacity() < onRoll) {
            throw new BadRequestException(grade.getGradeName() + "-" + section.getSectionName() + " has " + onRoll
                    + " students; the capacity cannot be lower than that", "CAPACITY_BELOW_ROLL");
        }
        section.setMaxCapacity(request.getMaxCapacity());
        Section saved = sectionRepository.save(section);
        auditService.logSuccess("section:" + sectionId, "UPDATE_SECTION",
                "Capacity of " + grade.getGradeName() + "-" + saved.getSectionName() + " set to " + saved.getMaxCapacity());
        return saved;
    }

    private static final java.util.Set<String> SECTION_REFS = java.util.Set.of("section_id", "from_section_id", "to_section_id");
    private static final java.util.Set<String> GRADE_REFS = java.util.Set.of("grade_id", "target_grade_id");

    /** Deletes a section that nothing refers to (no students now or in the past, no attendance, timetable, marks …). */
    @Transactional
    public void deleteSection(UUID sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        Grade grade = findGrade(section.getGradeId());
        java.util.Map<String, Long> uses = usageChecker.usage(sectionId, SECTION_REFS, java.util.Set.of("sections"));
        if (!uses.isEmpty()) {
            throw new ConflictException(grade.getGradeName() + "-" + section.getSectionName() + " cannot be deleted: it is used by "
                    + StructureUsageChecker.describe(uses) + ". Move or remove those first.");
        }
        sectionRepository.delete(section);
        auditService.logSuccess("section:" + sectionId, "DELETE_SECTION", "Deleted section " + grade.getGradeName() + "-" + section.getSectionName());
    }

    /** Deletes a class and its sections, only when neither the class nor any of its sections is used anywhere. */
    @Transactional
    public void deleteGrade(UUID gradeId) {
        Grade grade = findGrade(gradeId);
        java.util.Map<String, Long> uses = new java.util.LinkedHashMap<>(usageChecker.usage(gradeId, GRADE_REFS, java.util.Set.of("grades", "sections")));
        List<Section> own = sectionRepository.findByGradeId(gradeId);
        own.forEach(s -> usageChecker.usage(s.getSectionId(), SECTION_REFS, java.util.Set.of("sections")).forEach((k, v) -> uses.merge(k, v, Long::sum)));
        if (!uses.isEmpty()) {
            throw new ConflictException(grade.getGradeName() + " cannot be deleted: it is used by " + StructureUsageChecker.describe(uses)
                    + ". Move or remove those first.");
        }
        sectionRepository.deleteAll(own);
        gradeRepository.delete(grade);
        auditService.logSuccess("grade:" + gradeId, "DELETE_GRADE", "Deleted class " + grade.getGradeName() + " and " + own.size() + " empty sections");
    }

    @Transactional
    public Section assignClassTeacher(UUID sectionId, AssignClassTeacherRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        section.setClassTeacherId(request.getStaffId());
        section.setClassTeacherName(request.getStaffName().trim());
        Section saved = sectionRepository.save(section);
        auditService.logSuccess("section:" + saved.getSectionId(), "ASSIGN_CLASS_TEACHER",
                "Class teacher of section " + saved.getSectionName() + " set to " + saved.getClassTeacherName());
        return saved;
    }

    @Transactional
    public House createHouse(CreateHouseRequest request) {
        tenantGuard.requireTenant(request.getTenantId());
        String houseName = request.getHouseName().trim();
        if (houseRepository.findByTenantId(request.getTenantId()).stream().anyMatch(h -> h.getHouseName().equalsIgnoreCase(houseName))) {
            throw new ConflictException("A house named " + houseName + " already exists");
        }
        House house = House.builder()
                .tenantId(request.getTenantId())
                .houseName(houseName)
                .colorHex(request.getColorHex())
                .build();

        House saved = houseRepository.save(house);
        auditService.logSuccess("house:" + saved.getHouseId(), "CREATE_HOUSE", "Created house " + saved.getHouseName());
        return saved;
    }

    /** Renames a house or changes its colour; the name stays unique within the school. */
    @Transactional
    public House updateHouse(UUID houseId, UpdateHouseRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House", "houseId", houseId));
        tenantGuard.requireTenantRecord(house.getTenantId(), "House", "houseId", houseId);
        String houseName = request.getHouseName().trim();
        if (houseRepository.findByTenantId(house.getTenantId()).stream()
                .anyMatch(other -> !other.getHouseId().equals(houseId) && other.getHouseName().equalsIgnoreCase(houseName))) {
            throw new ConflictException("A house named " + houseName + " already exists");
        }
        house.setHouseName(houseName);
        house.setColorHex(request.getColorHex());
        House saved = houseRepository.save(house);
        auditService.logSuccess("house:" + houseId, "UPDATE_HOUSE", "House " + saved.getHouseName() + " updated");
        return saved;
    }

    /** Deletes a house that no student (current or past) and no activity credit refers to. */
    @Transactional
    public void deleteHouse(UUID houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House", "houseId", houseId));
        tenantGuard.requireTenantRecord(house.getTenantId(), "House", "houseId", houseId);
        java.util.Map<String, Long> uses = usageChecker.usage(houseId, java.util.Set.of("house_id"), java.util.Set.of("houses"));
        if (!uses.isEmpty()) {
            throw new ConflictException(house.getHouseName() + " house cannot be deleted: it is used by " + StructureUsageChecker.describe(uses)
                    + ". Move those students to another house first.");
        }
        houseRepository.delete(house);
        auditService.logSuccess("house:" + houseId, "DELETE_HOUSE", "Deleted house " + house.getHouseName());
    }

    @Transactional(readOnly = true)
    public List<House> listHouses(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        return houseRepository.findByTenantId(tenantId);
    }

    /** Loads a class of the caller's tenant; another tenant's class is reported as not found. */
    private Grade findGrade(UUID gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "gradeId", gradeId));
        tenantGuard.requireTenantRecord(grade.getTenantId(), "Grade", "gradeId", gradeId);
        return grade;
    }
}
