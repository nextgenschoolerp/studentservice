package com.kvs.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.*;
import com.kvs.student.enums.HoldType;
import com.kvs.student.enums.TransferRequestStatus;
import com.kvs.student.enums.VerificationStatus;
import com.kvs.student.service.AlumniService;
import com.kvs.student.service.StudentActivityService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering user stories US-03010 through US-03019:
 * - US-03010: Multi-criteria demographic filter
 * - US-03011: Bulk update student addresses & guardian contacts
 * - US-03012: Extracurricular credits & house points logging
 * - US-03013: Credential verification against alumni ledgers
 * - US-03014: Photo updates & batch photo directory export
 * - US-03015: Administrative holds (place, release, list)
 * - US-03016: Annual health metrics tracking (height, weight, BMI, vision)
 * - US-03017: Language preferences & elective course selections
 * - US-03018: Class enrollment roll list generation
 * - US-03019: Section transfer proposal & approval workflow
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentCapabilitiesBatch2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private StudentActivityService activityService;

    @MockBean
    private AlumniService alumniService;

    private String registrarToken;
    private String teacherToken;
    private String academicHeadToken;
    private String nurseToken;
    private String houseMasterToken;
    private String alumniCoordinatorToken;
    private String clerkToken;
    private String principalToken;

    @BeforeEach
    void setUp() {
        registrarToken = jwtTokenService.generateToken("registrar@kvs.local", Map.of("roles", List.of("ROLE_REGISTRAR")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.local", Map.of("roles", List.of("ROLE_CLASS_TEACHER")));
        academicHeadToken = jwtTokenService.generateToken("head@kvs.local", Map.of("roles", List.of("ROLE_ACADEMIC_HEAD")));
        nurseToken = jwtTokenService.generateToken("nurse@kvs.local", Map.of("roles", List.of("ROLE_SCHOOL_NURSE")));
        houseMasterToken = jwtTokenService.generateToken("house@kvs.local", Map.of("roles", List.of("ROLE_HOUSE_MASTER")));
        alumniCoordinatorToken = jwtTokenService.generateToken("alumni@kvs.local", Map.of("roles", List.of("ROLE_ALUMNI_COORDINATOR")));
        clerkToken = jwtTokenService.generateToken("clerk@kvs.local", Map.of("roles", List.of("ROLE_OFFICE_CLERK")));
        principalToken = jwtTokenService.generateToken("principal@kvs.local", Map.of("roles", List.of("ROLE_SCHOOL_PRINCIPAL")));
    }

    @Test
    @DisplayName("US-03010: GET /api/v1/students/search - Demographic filter returns filtered list")
    void testDemographicFilter_Success() throws Exception {
        StudentResponse student = StudentResponse.builder()
                .studentId(UUID.randomUUID())
                .studentUid("STU2026000010")
                .firstName("Rohan")
                .lastName("Verma")
                .transportRouteId("ROUTE-12")
                .build();

        when(studentService.filterStudents(any(), any(), any(), any(), any(), eq("ROUTE-12"), any()))
                .thenReturn(List.of(student));

        mockMvc.perform(get("/api/v1/students/search")
                        .param("transportRouteId", "ROUTE-12")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].studentUid").value("STU2026000010"))
                .andExpect(jsonPath("$.data[0].transportRouteId").value("ROUTE-12"));
    }

    @Test
    @DisplayName("US-03011: PUT /api/v1/students/bulk-contact-update - Bulk contact update returns updated count")
    void testBulkContactUpdate_Success() throws Exception {
        BulkContactUpdateRequest.ContactUpdateItem item = BulkContactUpdateRequest.ContactUpdateItem.builder()
                .studentId(UUID.randomUUID())
                .residentialAddress("45 MG Road, Indiranagar, Bangalore")
                .emergencyContactPhone("+91-9876543210")
                .build();
        BulkContactUpdateRequest request = BulkContactUpdateRequest.builder()
                .items(List.of(item))
                .build();

        when(studentService.bulkUpdateContacts(any(BulkContactUpdateRequest.class))).thenReturn(1);

        mockMvc.perform(put("/api/v1/students/bulk-contact-update")
                        .header("Authorization", "Bearer " + clerkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("US-03012: POST /api/v1/students/{studentId}/activities - Log extracurricular credits returns 201")
    void testLogActivityCredits_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        LogActivityCreditRequest request = LogActivityCreditRequest.builder()
                .activityName("Inter-School Debate Championship")
                .activityCategory("Literary")
                .pointsAwarded(15)
                .eventDate(LocalDate.now())
                .remarks("First Runner-up")
                .build();

        ActivityCreditResponse response = ActivityCreditResponse.builder()
                .creditId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000012")
                .studentName("Ananya Sen")
                .activityName("Inter-School Debate Championship")
                .pointsAwarded(15)
                .build();

        when(activityService.logActivityCredit(eq(studentId), any(LogActivityCreditRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/activities")
                        .header("Authorization", "Bearer " + houseMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pointsAwarded").value(15))
                .andExpect(jsonPath("$.data.activityName").value("Inter-School Debate Championship"));
    }

    @Test
    @DisplayName("US-03013: POST /api/v1/alumni/credential-verifications - Submit verification request returns 201")
    void testSubmitCredentialVerification_Success() throws Exception {
        SubmitCredentialVerificationRequest request = SubmitCredentialVerificationRequest.builder()
                .studentUid("STU2024000100")
                .requestorName("Infosys HR Background Verification")
                .requestorOrganization("Infosys Limited")
                .credentialType("TRANSFER_CERTIFICATE")
                .remarks("Verification for employment onboarding")
                .build();

        UUID requestId = UUID.randomUUID();
        CredentialVerificationResponse response = CredentialVerificationResponse.builder()
                .requestId(requestId)
                .studentUid("STU2024000100")
                .requestorName("Infosys HR Background Verification")
                .requestorOrganization("Infosys Limited")
                .credentialType("TRANSFER_CERTIFICATE")
                .status(VerificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(alumniService.submitCredentialVerification(any(SubmitCredentialVerificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/alumni/credential-verifications")
                        .header("Authorization", "Bearer " + alumniCoordinatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.studentUid").value("STU2024000100"));
    }

    @Test
    @DisplayName("US-03014: POST /api/v1/students/{studentId}/photo - Update student photo returns 200")
    void testUpdateStudentPhoto_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        UpdatePhotoRequest request = UpdatePhotoRequest.builder()
                .photoUrl("https://storage.kvs.local/photos/stu2026000014.jpg")
                .build();

        StudentResponse response = StudentResponse.builder()
                .studentId(studentId)
                .studentUid("STU2026000014")
                .profilePhotoUrl("https://storage.kvs.local/photos/stu2026000014.jpg")
                .build();

        when(studentService.updateStudentPhoto(eq(studentId), any(UpdatePhotoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/photo")
                        .header("Authorization", "Bearer " + academicHeadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profilePhotoUrl").value("https://storage.kvs.local/photos/stu2026000014.jpg"));
    }

    @Test
    @DisplayName("US-03015: POST /api/v1/students/{studentId}/holds - Place administrative hold returns 201")
    void testPlaceAdministrativeHold_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        PlaceHoldRequest request = PlaceHoldRequest.builder()
                .holdType(HoldType.FEE_DEFAULT)
                .reason("Term 2 tuition fee payment default exceeding 45 days")
                .build();

        AdministrativeHoldResponse response = AdministrativeHoldResponse.builder()
                .holdId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000015")
                .holdType(HoldType.FEE_DEFAULT)
                .reason("Term 2 tuition fee payment default exceeding 45 days")
                .isActive(true)
                .build();

        when(studentService.placeHold(eq(studentId), any(PlaceHoldRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/holds")
                        .header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.holdType").value("FEE_DEFAULT"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("US-03016: POST /api/v1/students/{studentId}/health-metrics - Track height/weight returns 201")
    void testRecordHealthMetric_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        RecordHealthMetricRequest request = RecordHealthMetricRequest.builder()
                .checkupDate(LocalDate.now())
                .heightCm(new BigDecimal("142.5"))
                .weightKg(new BigDecimal("36.0"))
                .visionLeft("6/6")
                .visionRight("6/6")
                .healthNotes("Normal vitals and healthy BMI")
                .build();

        HealthMetricResponse response = HealthMetricResponse.builder()
                .metricId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000016")
                .heightCm(new BigDecimal("142.5"))
                .weightKg(new BigDecimal("36.0"))
                .bmi(new BigDecimal("17.73"))
                .visionLeft("6/6")
                .visionRight("6/6")
                .build();

        when(studentService.recordHealthMetric(eq(studentId), any(RecordHealthMetricRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/health-metrics")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bmi").value(17.73));
    }

    @Test
    @DisplayName("US-03017: POST /api/v1/students/{studentId}/electives - Record elective preferences returns 201")
    void testRecordElectiveSelection_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        RecordElectiveSelectionRequest request = RecordElectiveSelectionRequest.builder()
                .academicYearId(yearId)
                .firstLanguage("English")
                .secondLanguage("Hindi")
                .thirdLanguage("Sanskrit")
                .electiveSubjectCode("CS-101")
                .build();

        ElectiveSelectionResponse response = ElectiveSelectionResponse.builder()
                .selectionId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000017")
                .academicYearId(yearId)
                .firstLanguage("English")
                .secondLanguage("Hindi")
                .thirdLanguage("Sanskrit")
                .electiveSubjectCode("CS-101")
                .status("CONFIRMED")
                .build();

        when(studentService.recordElectiveSelection(eq(studentId), any(RecordElectiveSelectionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/electives")
                        .header("Authorization", "Bearer " + clerkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.electiveSubjectCode").value("CS-101"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("US-03018: GET /api/v1/academic/sections/{sectionId}/roll-list - Roll list returns 200")
    void testGenerateRollList_Success() throws Exception {
        UUID sectionId = UUID.randomUUID();
        ClassRollListResponse.RollListStudentItem item = ClassRollListResponse.RollListStudentItem.builder()
                .studentId(UUID.randomUUID())
                .studentUid("STU2026000018")
                .rollNumber(1)
                .fullName("Aarush Das")
                .gender("MALE")
                .bloodGroup("O+")
                .houseName("Tagore")
                .emergencyContactPhone("+91-9876543211")
                .isOnHold(false)
                .build();

        ClassRollListResponse response = ClassRollListResponse.builder()
                .sectionId(sectionId)
                .sectionName("Section A")
                .gradeName("Grade 6")
                .generatedDate(LocalDate.now())
                .totalActiveStudents(1)
                .students(List.of(item))
                .build();

        when(studentService.getSectionRollList(sectionId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/academic/sections/" + sectionId + "/roll-list")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").value("Section A"))
                .andExpect(jsonPath("$.data.students[0].rollNumber").value(1));
    }

    @Test
    @DisplayName("US-03019: POST /api/v1/students/{studentId}/section-transfers - Propose transfer returns 201")
    void testProposeSectionTransfer_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID targetSectionId = UUID.randomUUID();
        SectionTransferProposalRequest request = SectionTransferProposalRequest.builder()
                .targetSectionId(targetSectionId)
                .reason("Balancing section student strengths and language lab schedules")
                .build();

        SectionTransferResponse response = SectionTransferResponse.builder()
                .transferId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000019")
                .toSectionId(targetSectionId)
                .toSectionName("Section B")
                .status(TransferRequestStatus.PENDING)
                .reason("Balancing section student strengths and language lab schedules")
                .build();

        when(studentService.proposeSectionTransfer(eq(studentId), any(SectionTransferProposalRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/section-transfers")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.toSectionName").value("Section B"));
    }

    @Test
    @DisplayName("US-03015: 403 Forbidden when unauthorized role attempts placing hold")
    void testPlaceAdministrativeHold_Forbidden() throws Exception {
        UUID studentId = UUID.randomUUID();
        PlaceHoldRequest request = PlaceHoldRequest.builder()
                .holdType(HoldType.DISCIPLINARY)
                .reason("Unauthorized action")
                .build();

        mockMvc.perform(post("/api/v1/students/" + studentId + "/holds")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-03010: 401 Unauthorized when no JWT token provided")
    void testDemographicFilter_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/students/search"))
                .andExpect(status().isUnauthorized());
    }
}
