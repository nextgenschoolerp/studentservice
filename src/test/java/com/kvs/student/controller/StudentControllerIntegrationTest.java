package com.kvs.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.*;
import com.kvs.student.enums.Gender;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for StudentController covering user stories:
 * - US-03000: Enrollment & Permanent Student UID
 * - US-03001: Medical alerts and emergency contact updates
 * - US-03002: Sibling profile linking
 * - US-03003: Academic section and house allocation
 * - US-03004: ID card generation with Barcode and QR code
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private StudentService studentService;

    private String registrarToken;
    private String nurseToken;
    private String teacherToken;
    private String unprivilegedToken;

    @BeforeEach
    void setUp() {
        registrarToken = jwtTokenService.generateToken("registrar@kvs.local", Map.of("roles", List.of("ROLE_REGISTRAR")));
        nurseToken = jwtTokenService.generateToken("nurse@kvs.local", Map.of("roles", List.of("ROLE_SCHOOL_NURSE")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.local", Map.of("roles", List.of("ROLE_CLASS_TEACHER")));
        unprivilegedToken = jwtTokenService.generateToken("guest@kvs.local", Map.of("roles", List.of("ROLE_GUEST")));
    }

    @Test
    @DisplayName("US-03000: POST /api/v1/students - Enroll student successfully returns 201 Created")
    void testEnrollStudent_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        EnrollStudentRequest request = EnrollStudentRequest.builder()
                .tenantId("DPS-DELHI")
                .branchId("MAIN-01")
                .firstName("Aarav")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(2015, 5, 14))
                .gender(Gender.MALE)
                .bloodGroup("O+")
                .medicalAlerts("Peanut allergy")
                .emergencyContactPhone("+919876543210")
                .guardianName("Rajesh Sharma")
                .guardianRelationship("Father")
                .guardianPhone("+919876543210")
                .build();

        StudentResponse response = StudentResponse.builder()
                .studentId(studentId)
                .tenantId("DPS-DELHI")
                .branchId("MAIN-01")
                .studentUid("STU2026000001")
                .firstName("Aarav")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(2015, 5, 14))
                .gender(Gender.MALE)
                .bloodGroup("O+")
                .studentStatus(StudentStatus.ACTIVE)
                .idCardBarcode("BC-STU2026000001")
                .idCardQrCode("https://verify.kvs.local/students/STU2026000001")
                .build();

        when(studentService.enrollStudent(any(EnrollStudentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + registrarToken)
                        .header("X-Trace-Id", "trace-sis-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "trace-sis-001"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentUid").value("STU2026000001"))
                .andExpect(jsonPath("$.data.firstName").value("Aarav"))
                .andExpect(jsonPath("$.data.studentStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("US-03000: POST /api/v1/students - Validation failure missing mandatory fields returns 400")
    void testEnrollStudent_ValidationFailure() throws Exception {
        EnrollStudentRequest invalidRequest = EnrollStudentRequest.builder()
                .tenantId("DPS-DELHI")
                // Missing branchId, firstName, lastName, dateOfBirth, gender
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-03000: POST /api/v1/students - Unauthenticated request returns 401")
    void testEnrollStudent_Unauthenticated() throws Exception {
        EnrollStudentRequest request = EnrollStudentRequest.builder()
                .tenantId("DPS-DELHI")
                .branchId("MAIN-01")
                .firstName("Aarav")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(2015, 5, 14))
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("US-03000: POST /api/v1/students - Unauthorized role returns 403 Forbidden")
    void testEnrollStudent_Forbidden() throws Exception {
        EnrollStudentRequest request = EnrollStudentRequest.builder()
                .tenantId("DPS-DELHI")
                .branchId("MAIN-01")
                .firstName("Aarav")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(2015, 5, 14))
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + unprivilegedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-03001: PUT /api/v1/students/{studentId}/medical - School nurse updates medical alerts returns 200")
    void testUpdateMedicalAlerts_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        UpdateMedicalAlertsRequest request = UpdateMedicalAlertsRequest.builder()
                .bloodGroup("B+")
                .medicalAlerts("Asthma inhaler required before physical exercise")
                .emergencyContactPhone("+919876543999")
                .build();

        StudentResponse response = StudentResponse.builder()
                .studentId(studentId)
                .studentUid("STU2026000001")
                .bloodGroup("B+")
                .medicalAlerts("Asthma inhaler required before physical exercise")
                .emergencyContactPhone("+919876543999")
                .build();

        when(studentService.updateMedicalAlerts(eq(studentId), any(UpdateMedicalAlertsRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/students/" + studentId + "/medical")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bloodGroup").value("B+"))
                .andExpect(jsonPath("$.data.medicalAlerts").value("Asthma inhaler required before physical exercise"));
    }

    @Test
    @DisplayName("US-03002: POST /api/v1/students/{studentId}/siblings - Link multi-sibling profile returns 200")
    void testLinkSibling_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID siblingId = UUID.randomUUID();
        LinkSiblingRequest request = LinkSiblingRequest.builder()
                .parentAccountId("PARENT-ACC-8899")
                .siblingStudentId(siblingId)
                .build();

        StudentResponse response = StudentResponse.builder()
                .studentId(studentId)
                .studentUid("STU2026000001")
                .parentAccountId("PARENT-ACC-8899")
                .build();

        when(studentService.linkSibling(eq(studentId), any(LinkSiblingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/siblings")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parentAccountId").value("PARENT-ACC-8899"));
    }

    @Test
    @DisplayName("US-03003: PUT /api/v1/students/{studentId}/allocation - Assign section and house returns 200")
    void testAssignSectionAndHouse_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();

        AssignSectionHouseRequest request = AssignSectionHouseRequest.builder()
                .gradeId(gradeId)
                .sectionId(sectionId)
                .houseId(houseId)
                .rollNumber(15)
                .build();

        StudentResponse response = StudentResponse.builder()
                .studentId(studentId)
                .studentUid("STU2026000001")
                .gradeId(gradeId)
                .gradeName("Grade 5")
                .sectionId(sectionId)
                .sectionName("Section A")
                .houseId(houseId)
                .houseName("Red Falcons")
                .rollNumber(15)
                .build();

        when(studentService.assignSectionAndHouse(eq(studentId), any(AssignSectionHouseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/students/" + studentId + "/allocation")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").value("Section A"))
                .andExpect(jsonPath("$.data.houseName").value("Red Falcons"))
                .andExpect(jsonPath("$.data.rollNumber").value(15));
    }

    @Test
    @DisplayName("US-03004: GET /api/v1/students/{studentId}/id-card - Generate ID card data returns 200")
    void testGenerateIdCard_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        IdCardResponse response = IdCardResponse.builder()
                .studentId(studentId)
                .studentUid("STU2026000001")
                .fullName("Aarav Sharma")
                .gradeName("Grade 5")
                .sectionName("Section A")
                .houseName("Red Falcons")
                .barcodeData("BC-STU2026000001")
                .qrCodeData("https://verify.kvs.local/students/STU2026000001")
                .verificationUrl("https://verify.kvs.local/students/STU2026000001")
                .build();

        when(studentService.generateIdCard(studentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/students/" + studentId + "/id-card")
                        .header("Authorization", "Bearer " + registrarToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.barcodeData").value("BC-STU2026000001"))
                .andExpect(jsonPath("$.data.qrCodeData").value("https://verify.kvs.local/students/STU2026000001"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{studentId} - Fetch student by primary ID returns 200")
    void testGetStudent_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        StudentResponse response = StudentResponse.builder()
                .studentId(studentId)
                .studentUid("STU2026000001")
                .firstName("Aarav")
                .lastName("Sharma")
                .studentStatus(StudentStatus.ACTIVE)
                .build();

        when(studentService.getStudent(studentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/students/" + studentId)
                        .header("Authorization", "Bearer " + registrarToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentUid").value("STU2026000001"));
    }

    @Test
    @DisplayName("GET /api/v1/students/uid/{studentUid} - Fetch student by UID returns 200")
    void testGetStudentByUid_Success() throws Exception {
        StudentResponse response = StudentResponse.builder()
                .studentId(UUID.randomUUID())
                .studentUid("STU2026000001")
                .firstName("Aarav")
                .lastName("Sharma")
                .studentStatus(StudentStatus.ACTIVE)
                .build();

        when(studentService.getStudentByUid("STU2026000001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/students/uid/STU2026000001")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentUid").value("STU2026000001"));
    }

    @Test
    @DisplayName("GET /api/v1/students - List students by tenant and branch returns 200")
    void testListStudents_Success() throws Exception {
        StudentResponse student = StudentResponse.builder()
                .studentId(UUID.randomUUID())
                .studentUid("STU2026000001")
                .firstName("Aarav")
                .lastName("Sharma")
                .studentStatus(StudentStatus.ACTIVE)
                .build();

        when(studentService.listStudents("DPS-DELHI", "MAIN-01")).thenReturn(List.of(student));

        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + registrarToken)
                        .param("tenantId", "DPS-DELHI")
                        .param("branchId", "MAIN-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].studentUid").value("STU2026000001"));
    }
}
