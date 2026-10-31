package com.kvs.student.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.SaveAttendanceRequest;
import com.kvs.student.dto.SaveAttendanceSettingsRequest;
import com.kvs.student.dto.UpdateFollowUpRequest;
import com.kvs.student.entity.Grade;
import com.kvs.student.entity.Section;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.*;
import com.kvs.student.repo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end (real services, H2) tests of daily student attendance: AM/PM registers, absence reasons,
 * guardian follow-ups, the school's day figure and trend, a student's attendance to date, roles and tenants.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttendanceIntegrationTest {

    private static final String TENANT = "TENANT-ATT";
    private static final String OTHER_TENANT = "TENANT-ATT-OTHER";
    private static final String BRANCH = "MAIN-01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private StudentMasterRepository studentRepository;
    @Autowired private GradeRepository gradeRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private StudentAttendanceRepository attendanceRepository;
    @Autowired private AttendanceRegisterRepository registerRepository;
    @Autowired private com.kvs.student.repo.AttendanceSettingsRepository settingsRepository;

    private Section sectionA;
    private Section sectionB;
    private Section otherTenantSection;
    private StudentMaster asha, bilal, chitra, dev;   // 6-A, roll 1-3 and 6-B
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        Grade grade = gradeRepository.save(Grade.builder().tenantId(TENANT).gradeName("Class 6").gradeCode("ATT-06").sequenceOrder(9).build());
        sectionA = sectionRepository.save(Section.builder().gradeId(grade.getGradeId()).sectionName("A").build());
        sectionB = sectionRepository.save(Section.builder().gradeId(grade.getGradeId()).sectionName("B").build());
        Grade otherGrade = gradeRepository.save(Grade.builder().tenantId(OTHER_TENANT).gradeName("Class 6").gradeCode("ATT-06").sequenceOrder(9).build());
        otherTenantSection = sectionRepository.save(Section.builder().gradeId(otherGrade.getGradeId()).sectionName("A").build());

        chitra = studentRepository.save(student("Chitra", grade, sectionA, 3));
        asha = studentRepository.save(student("Asha", grade, sectionA, 1));
        bilal = studentRepository.save(student("Bilal", grade, sectionA, 2));
        dev = studentRepository.save(student("Dev", grade, sectionB, 1));
        StudentMaster left = student("Left", grade, sectionA, 4);
        left.setStudentStatus(StudentStatus.TRANSFERRED);
        studentRepository.save(left);
    }

    @AfterEach
    void tearDown() {
        attendanceRepository.deleteAll();
        registerRepository.deleteAll();
        settingsRepository.deleteAll();
        studentRepository.deleteAll();
        sectionRepository.deleteAll();
        gradeRepository.deleteAll();
    }

    private static StudentMaster student(String firstName, Grade grade, Section section, int roll) {
        return StudentMaster.builder().tenantId(grade.getTenantId()).branchId(BRANCH)
                .studentUid("ATT-" + UUID.randomUUID().toString().substring(0, 8)).firstName(firstName).lastName("Rao")
                .dateOfBirth(LocalDate.of(2014, 3, 9)).gender(Gender.FEMALE).studentStatus(StudentStatus.ACTIVE)
                .gradeId(grade.getGradeId()).sectionId(section.getSectionId()).rollNumber(roll).emergencyContactPhone("+91-9800000000")
                .build();
    }

    private String token(String role) {
        return token(role, TENANT);
    }

    private String token(String role, String tenantId) {
        return "Bearer " + jwtTokenService.generateToken(role.toLowerCase() + "@kvs.local", Map.of("roles", List.of("ROLE_" + role), "tenantId", tenantId));
    }

    private static SaveAttendanceRequest.Entry mark(StudentMaster s, AttendanceStatus status, String reason) {
        return SaveAttendanceRequest.Entry.builder().studentId(s.getStudentId()).status(status).reason(reason).build();
    }

    private ResultActions save(String role, UUID sectionId, LocalDate date, AttendanceSession session, SaveAttendanceRequest.Entry... entries) throws Exception {
        SaveAttendanceRequest body = SaveAttendanceRequest.builder().date(date).session(session).entries(List.of(entries)).build();
        return mockMvc.perform(put("/api/v1/attendance/sections/{id}/register", sectionId)
                .header("Authorization", token(role)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /** AM register of 6-A today: Asha present, Bilal excused (fever), Chitra unexcused. */
    private void markMorning() throws Exception {
        save("CLASS_TEACHER", sectionA.getSectionId(), today, AttendanceSession.AM,
                mark(asha, AttendanceStatus.PRESENT, null), mark(bilal, AttendanceStatus.EXCUSED, "Fever"), mark(chitra, AttendanceStatus.UNEXCUSED, null))
                .andExpect(status().isOk());
    }

    private String chitraAbsenceId() throws Exception {
        String json = mockMvc.perform(get("/api/v1/attendance/follow-ups").header("Authorization", token("OFFICE_CLERK")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).get("data");
        return data.get(0).get("attendanceId").asText();
    }

    @Test
    @DisplayName("GET register - students on roll in roll-number order, unmarked, register not submitted")
    void testRegister_EmptyDay() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/sections/{id}/register", sectionA.getSectionId()).header("Authorization", token("CLASS_TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeName").value("Class 6"))
                .andExpect(jsonPath("$.data.sectionName").value("A"))
                .andExpect(jsonPath("$.data.date").value(today.toString()))
                .andExpect(jsonPath("$.data.students", hasSize(3)))
                .andExpect(jsonPath("$.data.students[*].fullName", contains("Asha Rao", "Bilal Rao", "Chitra Rao")))
                .andExpect(jsonPath("$.data.students[0].am").doesNotExist())
                .andExpect(jsonPath("$.data.am.submitted").value(false))
                .andExpect(jsonPath("$.data.am.unmarked").value(3));
    }

    @Test
    @DisplayName("PUT register - AM marks saved, counts and submitter returned, unexcused opens a follow-up")
    void testSaveRegister_Morning() throws Exception {
        save("CLASS_TEACHER", sectionA.getSectionId(), today, AttendanceSession.AM,
                mark(asha, AttendanceStatus.PRESENT, null), mark(bilal, AttendanceStatus.EXCUSED, "Fever"), mark(chitra, AttendanceStatus.UNEXCUSED, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Attendance register saved"))
                .andExpect(jsonPath("$.data.am.submitted").value(true))
                .andExpect(jsonPath("$.data.am.submittedBy").value("class_teacher@kvs.local"))
                .andExpect(jsonPath("$.data.am.present").value(1))
                .andExpect(jsonPath("$.data.am.excused").value(1))
                .andExpect(jsonPath("$.data.am.unexcused").value(1))
                .andExpect(jsonPath("$.data.am.unmarked").value(0))
                .andExpect(jsonPath("$.data.pm.submitted").value(false))
                .andExpect(jsonPath("$.data.students[1].am.reason").value("Fever"))
                .andExpect(jsonPath("$.data.students[2].am.status").value("UNEXCUSED"))
                .andExpect(jsonPath("$.data.students[2].am.followUpStatus").value("OPEN"));
    }

    @Test
    @DisplayName("PUT register - excused without a reason, a student of another section, a repeat or a future date returns 400")
    void testSaveRegister_Invalid() throws Exception {
        save("CLASS_TEACHER", sectionA.getSectionId(), today, AttendanceSession.AM, mark(asha, AttendanceStatus.EXCUSED, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("needs a reason")));
        save("CLASS_TEACHER", sectionA.getSectionId(), today, AttendanceSession.AM, mark(dev, AttendanceStatus.PRESENT, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not on the roll")));
        save("CLASS_TEACHER", sectionA.getSectionId(), today, AttendanceSession.AM, mark(asha, AttendanceStatus.PRESENT, null), mark(asha, AttendanceStatus.LATE, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("listed twice")));
        save("CLASS_TEACHER", sectionA.getSectionId(), today.plusDays(1), AttendanceSession.AM, mark(asha, AttendanceStatus.PRESENT, null))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/attendance/sections/{id}/register", sectionA.getSectionId()).header("Authorization", token("CLASS_TEACHER")))
                .andExpect(jsonPath("$.data.am.submitted").value(false));
    }

    @Test
    @DisplayName("PUT register - a teacher may not correct a register older than 7 days; a principal may")
    void testSaveRegister_OldCorrection() throws Exception {
        LocalDate old = today.minusDays(8);
        save("CLASS_TEACHER", sectionA.getSectionId(), old, AttendanceSession.AM, mark(asha, AttendanceStatus.PRESENT, null))
                .andExpect(status().isForbidden());
        save("SCHOOL_PRINCIPAL", sectionA.getSectionId(), old, AttendanceSession.AM, mark(asha, AttendanceStatus.PRESENT, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.am.submittedBy").value("school_principal@kvs.local"));
    }

    @Test
    @DisplayName("PUT register again - correction recorded as a change; unexcused → excused closes the follow-up")
    void testSaveRegister_Correction() throws Exception {
        markMorning();
        save("VICE_PRINCIPAL", sectionA.getSectionId(), today, AttendanceSession.AM, mark(chitra, AttendanceStatus.EXCUSED, "Guardian called: family function"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.am.submittedBy").value("class_teacher@kvs.local"))
                .andExpect(jsonPath("$.data.am.lastChangedBy").value("vice_principal@kvs.local"))
                .andExpect(jsonPath("$.data.am.excused").value(2))
                .andExpect(jsonPath("$.data.students[2].am.followUpStatus").doesNotExist());
        mockMvc.perform(get("/api/v1/attendance/follow-ups").header("Authorization", token("OFFICE_CLERK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET summary - today's figure from the AM registers: present, rate, registers submitted, open follow-ups")
    void testSummary() throws Exception {
        markMorning();
        mockMvc.perform(get("/api/v1/attendance/summary").header("Authorization", token("SCHOOL_PRINCIPAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(4))
                .andExpect(jsonPath("$.data.present").value(1))
                .andExpect(jsonPath("$.data.excused").value(1))
                .andExpect(jsonPath("$.data.unexcused").value(1))
                .andExpect(jsonPath("$.data.unmarked").value(1))
                .andExpect(jsonPath("$.data.presentRate").value(33.3))
                .andExpect(jsonPath("$.data.registersExpected").value(2))
                .andExpect(jsonPath("$.data.amRegistersSubmitted").value(1))
                .andExpect(jsonPath("$.data.pmRegistersSubmitted").value(0))
                .andExpect(jsonPath("$.data.openFollowUps").value(1));
    }

    @Test
    @DisplayName("GET trend - only school days (an AM register submitted) in the range")
    void testTrend() throws Exception {
        save("CLASS_TEACHER", sectionA.getSectionId(), today.minusDays(2), AttendanceSession.AM,
                mark(asha, AttendanceStatus.PRESENT, null), mark(bilal, AttendanceStatus.PRESENT, null), mark(chitra, AttendanceStatus.LATE, "Bus late"))
                .andExpect(status().isOk());
        markMorning();
        mockMvc.perform(get("/api/v1/attendance/trend").param("from", today.minusDays(4).toString()).header("Authorization", token("REGISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].date").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.data[0].present").value(3))
                .andExpect(jsonPath("$.data[0].late").value(1))
                .andExpect(jsonPath("$.data[0].presentRate").value(100.0))
                .andExpect(jsonPath("$.data[1].date").value(today.toString()));
        mockMvc.perform(get("/api/v1/attendance/trend").param("from", today.toString()).param("to", today.minusDays(1).toString())
                        .header("Authorization", token("REGISTRAR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Follow-ups - unexcused absence listed with the guardian contact; PATCH records the call; invalid updates rejected")
    void testFollowUps() throws Exception {
        markMorning();
        mockMvc.perform(get("/api/v1/attendance/follow-ups").header("Authorization", token("OFFICE_CLERK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fullName").value("Chitra Rao"))
                .andExpect(jsonPath("$.data[0].gradeName").value("Class 6"))
                .andExpect(jsonPath("$.data[0].sectionName").value("A"))
                .andExpect(jsonPath("$.data[0].session").value("AM"))
                .andExpect(jsonPath("$.data[0].followUpStatus").value("OPEN"))
                .andExpect(jsonPath("$.data[0].emergencyContactPhone").value("+91-9800000000"));
        String id = chitraAbsenceId();

        UpdateFollowUpRequest called = UpdateFollowUpRequest.builder().followUpStatus(FollowUpStatus.GUARDIAN_CONTACTED).note("Mother says she is unwell").build();
        mockMvc.perform(patch("/api/v1/attendance/records/{id}/follow-up", id).header("Authorization", token("OFFICE_CLERK"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(called)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followUpStatus").value("GUARDIAN_CONTACTED"))
                .andExpect(jsonPath("$.data.followUpNote").value("Mother says she is unwell"))
                .andExpect(jsonPath("$.data.followUpBy").value("office_clerk@kvs.local"));

        UpdateFollowUpRequest reopen = UpdateFollowUpRequest.builder().followUpStatus(FollowUpStatus.OPEN).note("x").build();
        mockMvc.perform(patch("/api/v1/attendance/records/{id}/follow-up", id).header("Authorization", token("OFFICE_CLERK"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reopen)))
                .andExpect(status().isBadRequest());
        UpdateFollowUpRequest noNote = UpdateFollowUpRequest.builder().followUpStatus(FollowUpStatus.RESOLVED).note(" ").build();
        mockMvc.perform(patch("/api/v1/attendance/records/{id}/follow-up", id).header("Authorization", token("OFFICE_CLERK"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(noNote)))
                .andExpect(status().isBadRequest());

        UpdateFollowUpRequest resolved = UpdateFollowUpRequest.builder().followUpStatus(FollowUpStatus.RESOLVED).note("Medical note received").build();
        mockMvc.perform(patch("/api/v1/attendance/records/{id}/follow-up", id).header("Authorization", token("CLASS_TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(resolved)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/attendance/follow-ups").header("Authorization", token("OFFICE_CLERK")))
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/v1/attendance/follow-ups").param("includeResolved", "true").header("Authorization", token("OFFICE_CLERK")))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("PATCH follow-up of a present mark returns 409")
    void testFollowUp_NotAbsence() throws Exception {
        markMorning();
        String asha = mockMvc.perform(get("/api/v1/attendance/sections/{id}/register", sectionA.getSectionId()).header("Authorization", token("CLASS_TEACHER")))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(asha).at("/data/students/0/am/attendanceId").asText();
        UpdateFollowUpRequest body = UpdateFollowUpRequest.builder().followUpStatus(FollowUpStatus.RESOLVED).note("n/a").build();
        mockMvc.perform(patch("/api/v1/attendance/records/{id}/follow-up", id).header("Authorization", token("OFFICE_CLERK"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET absences - late and absent marks, newest first, filtered by status and section")
    void testAbsences() throws Exception {
        save("CLASS_TEACHER", sectionA.getSectionId(), today.minusDays(1), AttendanceSession.AM, mark(asha, AttendanceStatus.LATE, "Bus late")).andExpect(status().isOk());
        markMorning();
        mockMvc.perform(get("/api/v1/attendance/absences").header("Authorization", token("REGISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data[2].status").value("LATE"));
        mockMvc.perform(get("/api/v1/attendance/absences").param("status", "EXCUSED").param("sectionId", sectionA.getSectionId().toString())
                        .header("Authorization", token("REGISTRAR")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fullName").value("Bilal Rao"))
                .andExpect(jsonPath("$.data[0].reason").value("Fever"));
        mockMvc.perform(get("/api/v1/attendance/absences").param("sectionId", sectionB.getSectionId().toString()).header("Authorization", token("REGISTRAR")))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET students/{id} - attendance rate to date, today's marks and absences")
    void testStudentSummary() throws Exception {
        mockMvc.perform(put("/api/v1/attendance/settings").header("Authorization", token("SCHOOL_PRINCIPAL")).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SaveAttendanceSettingsRequest("AM_PM")))).andExpect(status().isOk());
        save("CLASS_TEACHER", sectionA.getSectionId(), today.minusDays(1), AttendanceSession.AM, mark(bilal, AttendanceStatus.PRESENT, null)).andExpect(status().isOk());
        save("CLASS_TEACHER", sectionA.getSectionId(), today.minusDays(1), AttendanceSession.PM, mark(bilal, AttendanceStatus.PRESENT, null)).andExpect(status().isOk());
        markMorning();
        mockMvc.perform(get("/api/v1/attendance/students/{id}", bilal.getStudentId()).header("Authorization", token("CLASS_TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionsMarked").value(3))
                .andExpect(jsonPath("$.data.present").value(2))
                .andExpect(jsonPath("$.data.excused").value(1))
                .andExpect(jsonPath("$.data.attendanceRate").value(66.7))
                .andExpect(jsonPath("$.data.todayAm").value("EXCUSED"))
                .andExpect(jsonPath("$.data.todayPm").doesNotExist())
                .andExpect(jsonPath("$.data.absences", hasSize(1)))
                .andExpect(jsonPath("$.data.absences[0].reason").value("Fever"));
    }

    @Test
    @DisplayName("Roles - a registrar may view but not mark; a nurse may not view")
    void testRoles() throws Exception {
        save("REGISTRAR", sectionA.getSectionId(), today, AttendanceSession.AM, mark(asha, AttendanceStatus.PRESENT, null))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/attendance/sections/{id}/register", sectionA.getSectionId()).header("Authorization", token("REGISTRAR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/attendance/summary").header("Authorization", token("SCHOOL_NURSE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tenants - another tenant's section is not found; naming another tenant is forbidden; its data is not counted")
    void testTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/sections/{id}/register", otherTenantSection.getSectionId()).header("Authorization", token("CLASS_TEACHER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/attendance/summary").param("tenantId", OTHER_TENANT).header("Authorization", token("SCHOOL_PRINCIPAL")))
                .andExpect(status().isForbidden());
        markMorning();
        mockMvc.perform(get("/api/v1/attendance/summary").header("Authorization", token("SCHOOL_PRINCIPAL", OTHER_TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(0))
                .andExpect(jsonPath("$.data.present").value(0));
        mockMvc.perform(get("/api/v1/attendance/students/{id}", asha.getStudentId()).header("Authorization", token("CLASS_TEACHER", OTHER_TENANT)))
                .andExpect(status().isNotFound());
    }
}
