package com.kvs.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.*;
import com.kvs.student.enums.CertificateType;
import com.kvs.student.enums.Gender;
import com.kvs.student.enums.HoldType;
import com.kvs.student.enums.IncidentSeverity;
import com.kvs.student.enums.VerificationStatus;
import com.kvs.student.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scenario Actor Access Test for US-03020 through US-03100.
 * Verifies that role-based access control adheres to platform security specifications:
 * - Granted roles return 2xx successful.
 * - Non-granted roles return 403 Forbidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioActorAccessTest {

    private static final UUID STU_ID = UUID.fromString("6eed0003-0001-4000-8000-000000000001");
    private static final UUID SEC_ID = UUID.fromString("6eed0003-0002-4000-8000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private AcademicHierarchyService academicService;

    @MockBean
    private CertificateService certificateService;

    @MockBean
    private DisciplinaryService disciplinaryService;

    @MockBean
    private AlumniService alumniService;

    @MockBean
    private StudentActivityService activityService;

    @BeforeEach
    void setupMocks() {
        // Stub mock service responses so valid authorized requests return 2xx
        when(studentService.enrollStudent(any())).thenReturn(StudentResponse.builder().studentId(STU_ID).studentUid("STU2026000001").build());
        when(studentService.updateMedicalAlerts(eq(STU_ID), any())).thenReturn(StudentResponse.builder().studentId(STU_ID).build());
        when(studentService.linkSibling(eq(STU_ID), any())).thenReturn(StudentResponse.builder().studentId(STU_ID).build());
        when(studentService.assignSectionAndHouse(eq(STU_ID), any())).thenReturn(StudentResponse.builder().studentId(STU_ID).build());
        when(studentService.generateIdCard(eq(STU_ID))).thenReturn(IdCardResponse.builder().studentId(STU_ID).build());
        when(studentService.batchPromoteStudents(any())).thenReturn(BatchPromotionResponse.builder().status("SUCCESS").build());
        when(certificateService.issueTransferCertificate(eq(STU_ID), any())).thenReturn(CertificateResponse.builder().certificateId(UUID.randomUUID()).build());
        when(certificateService.issueBonafideCertificate(eq(STU_ID), any())).thenReturn(CertificateResponse.builder().certificateId(UUID.randomUUID()).build());
        when(disciplinaryService.recordIncident(eq(STU_ID), any())).thenReturn(DisciplinaryIncidentResponse.builder().incidentId(UUID.randomUUID()).build());
        when(alumniService.archiveAlumni(any())).thenReturn(AlumniResponse.builder().alumniId(UUID.randomUUID()).build());

        when(studentService.filterStudents(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(StudentResponse.builder().studentId(STU_ID).build()));
        when(studentService.bulkUpdateContacts(any())).thenReturn(1);
        when(activityService.logActivityCredit(eq(STU_ID), any())).thenReturn(ActivityCreditResponse.builder().creditId(UUID.randomUUID()).build());
        when(alumniService.submitCredentialVerification(any())).thenReturn(CredentialVerificationResponse.builder().requestId(UUID.randomUUID()).build());
        when(studentService.updateStudentPhoto(eq(STU_ID), any())).thenReturn(StudentResponse.builder().studentId(STU_ID).build());
        when(studentService.placeHold(eq(STU_ID), any())).thenReturn(AdministrativeHoldResponse.builder().holdId(UUID.randomUUID()).build());
        when(studentService.recordHealthMetric(eq(STU_ID), any())).thenReturn(HealthMetricResponse.builder().metricId(UUID.randomUUID()).build());
        when(studentService.recordElectiveSelection(eq(STU_ID), any())).thenReturn(ElectiveSelectionResponse.builder().selectionId(UUID.randomUUID()).build());
        when(studentService.getSectionRollList(eq(SEC_ID))).thenReturn(ClassRollListResponse.builder().sectionId(SEC_ID).students(List.of()).build());
        when(studentService.proposeSectionTransfer(eq(STU_ID), any())).thenReturn(SectionTransferResponse.builder().transferId(UUID.randomUUID()).build());
    }

    private static final String[] ROTATING_ROLES = {
            "ROLE_REGISTRAR",          // 0
            "ROLE_OFFICE_CLERK",        // 1
            "ROLE_CLASS_TEACHER",       // 2
            "ROLE_SCHOOL_NURSE",        // 3
            "ROLE_HOUSE_MASTER",        // 4
            "ROLE_ALUMNI_COORDINATOR",  // 5
            "ROLE_ACADEMIC_HEAD",       // 6
            "ROLE_SCHOOL_PRINCIPAL"     // 7
    };

    private static final String[] CAPABILITIES = {
            "STUDENT_UID_GENERATION",            // 0
            "MEDICAL_ALERTS_EMERGENCY_UPDATE",   // 1
            "SIBLING_LINKING",                   // 2
            "SECTION_HOUSE_ASSIGNMENT",          // 3
            "ID_CARD_PRINTING",                  // 4
            "BATCH_PROMOTION",                   // 5
            "TRANSFER_CERTIFICATE_DUAL_SIGNOFF", // 6
            "BONAFIDE_CHARACTER_CERTIFICATE",    // 7
            "DISCIPLINARY_INCIDENT_COUNSELING",  // 8
            "ALUMNI_ARCHIVING",                  // 9
            "DEMOGRAPHIC_MULTI_FILTER",          // 10
            "BULK_CONTACT_UPDATE",               // 11
            "ACTIVITY_CREDITS_HOUSE_POINTS",     // 12
            "CREDENTIAL_VERIFICATION",           // 13
            "PHOTO_MANAGEMENT_BATCH_EXPORT",     // 14
            "ADMINISTRATIVE_HOLD",               // 15
            "HEALTH_METRICS_TRACKING",           // 16
            "ELECTIVE_SELECTIONS",               // 17
            "CLASS_ROLL_LIST",                   // 18
            "SECTION_TRANSFER_REQUEST"           // 19
    };

    private static final Map<Integer, Set<String>> ALLOWED_ROLES = Map.ofEntries(
            Map.entry(0, Set.of("ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_OFFICE_CLERK")),
            Map.entry(1, Set.of("ROLE_SCHOOL_NURSE", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(2, Set.of("ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_OFFICE_CLERK")),
            Map.entry(3, Set.of("ROLE_REGISTRAR", "ROLE_CLASS_TEACHER", "ROLE_SUPER_ADMIN", "ROLE_HOUSE_MASTER")),
            Map.entry(4, Set.of("ROLE_OFFICE_CLERK", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_CLASS_TEACHER")),
            Map.entry(5, Set.of("ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_SCHOOL_PRINCIPAL")),
            Map.entry(6, Set.of("ROLE_SCHOOL_PRINCIPAL", "ROLE_VICE_PRINCIPAL", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(7, Set.of("ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_OFFICE_CLERK", "ROLE_SCHOOL_PRINCIPAL")),
            Map.entry(8, Set.of("ROLE_CLASS_TEACHER", "ROLE_SCHOOL_PRINCIPAL", "ROLE_VICE_PRINCIPAL", "ROLE_SUPER_ADMIN")),
            Map.entry(9, Set.of("ROLE_ALUMNI_COORDINATOR", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(10, Set.of("ROLE_CLASS_TEACHER", "ROLE_ACADEMIC_HEAD", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_OFFICE_CLERK")),
            Map.entry(11, Set.of("ROLE_SCHOOL_NURSE", "ROLE_SCHOOL_PRINCIPAL", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_OFFICE_CLERK")),
            Map.entry(12, Set.of("ROLE_HOUSE_MASTER", "ROLE_CLASS_TEACHER", "ROLE_SUPER_ADMIN", "ROLE_REGISTRAR")),
            Map.entry(13, Set.of("ROLE_ALUMNI_COORDINATOR", "ROLE_OFFICE_CLERK", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(14, Set.of("ROLE_ACADEMIC_HEAD", "ROLE_CLASS_TEACHER", "ROLE_OFFICE_CLERK", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(15, Set.of("ROLE_SCHOOL_PRINCIPAL", "ROLE_SCHOOL_NURSE", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(16, Set.of("ROLE_SCHOOL_REGISTRAR", "ROLE_REGISTRAR", "ROLE_HOUSE_MASTER", "ROLE_SCHOOL_NURSE", "ROLE_SUPER_ADMIN")),
            Map.entry(17, Set.of("ROLE_OFFICE_CLERK", "ROLE_ALUMNI_COORDINATOR", "ROLE_CLASS_TEACHER", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN")),
            Map.entry(18, Set.of("ROLE_CLASS_TEACHER", "ROLE_ACADEMIC_HEAD", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_OFFICE_CLERK", "ROLE_SCHOOL_PRINCIPAL")),
            Map.entry(19, Set.of("ROLE_SCHOOL_NURSE", "ROLE_ACADEMIC_HEAD", "ROLE_SCHOOL_PRINCIPAL", "ROLE_REGISTRAR", "ROLE_SUPER_ADMIN", "ROLE_CLASS_TEACHER"))
    );

    static Stream<Arguments> storyParameters() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int storyNum = 3020; storyNum <= 3100; storyNum++) {
            int storyIndex = storyNum - 3000;
            int capIndex = storyIndex % 20;
            int roleIndex = storyIndex % 8;
            String storyId = String.format("US-%05d", storyNum);
            String capName = CAPABILITIES[capIndex];
            String role = ROTATING_ROLES[roleIndex];
            boolean isAllowed = ALLOWED_ROLES.get(capIndex).contains(role);
            builder.add(Arguments.of(storyId, capIndex, capName, role, isAllowed));
        }
        return builder.build();
    }

    @ParameterizedTest(name = "{0}: {2} by {3} -> allowed={4}")
    @MethodSource("storyParameters")
    void testStoryRoleAccess(String storyId, int capIndex, String capName, String role, boolean isAllowed) throws Exception {
        String token = jwtTokenService.generateToken("user_" + role.toLowerCase() + "@kvs.local", Map.of("roles", List.of(role)));
        MockHttpServletRequestBuilder requestBuilder = buildRequestForCapability(capIndex);

        if (isAllowed) {
            mockMvc.perform(requestBuilder.header("Authorization", "Bearer " + token))
                    .andExpect(status().is2xxSuccessful());
        } else {
            mockMvc.perform(requestBuilder.header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    private MockHttpServletRequestBuilder buildRequestForCapability(int capIndex) throws Exception {
        return switch (capIndex) {
            case 0 -> post("/api/v1/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(EnrollStudentRequest.builder()
                            .tenantId("default").branchId("main").firstName("Aarav").lastName("Sharma")
                            .dateOfBirth(LocalDate.of(2015, 5, 20)).gender(Gender.MALE)
                            .emergencyContactPhone("+91-9876543210")
                            .guardianName("Sunil Sharma").guardianRelationship("Father")
                            .guardianPhone("+91-9876543210").build()));
            case 1 -> put("/api/v1/students/" + STU_ID + "/medical")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(UpdateMedicalAlertsRequest.builder()
                            .bloodGroup("B+").medicalAlerts("None").emergencyContactPhone("+91-9876543210").build()));
            case 2 -> post("/api/v1/students/" + STU_ID + "/siblings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LinkSiblingRequest.builder()
                            .parentAccountId("PARENT-1").siblingStudentId(UUID.randomUUID()).build()));
            case 3 -> put("/api/v1/students/" + STU_ID + "/allocation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(AssignSectionHouseRequest.builder()
                            .gradeId(UUID.randomUUID()).sectionId(SEC_ID).rollNumber(1).build()));
            case 4 -> get("/api/v1/students/" + STU_ID + "/id-card");
            case 5 -> post("/api/v1/academic/batch-promotion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(BatchPromotionRequest.builder()
                            .currentAcademicYearId(UUID.randomUUID()).fromGradeId(UUID.randomUUID())
                            .toGradeId(UUID.randomUUID()).nextAcademicYearId(UUID.randomUUID()).build()));
            case 6 -> post("/api/v1/students/" + STU_ID + "/certificates/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(IssueTransferCertificateRequest.builder()
                            .reasonForLeaving("Relocation").conductRemarks("Good conduct")
                            .secondSignOffUserId("principal@kvs.local").build()));
            case 7 -> post("/api/v1/students/" + STU_ID + "/certificates/bonafide")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(IssueBonafideCertificateRequest.builder()
                            .certificateType(CertificateType.BONAFIDE).purpose("Visa").build()));
            case 8 -> post("/api/v1/students/" + STU_ID + "/disciplinary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RecordDisciplinaryIncidentRequest.builder()
                            .incidentDate(LocalDate.now()).incidentCategory("Discipline").description("Disruption").severity(IncidentSeverity.MINOR).build()));
            case 9 -> post("/api/v1/alumni/archive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ArchiveAlumniRequest.builder()
                            .studentId(STU_ID).graduationYear(2026).finalGradePassed("Grade 12").build()));
            case 10 -> get("/api/v1/students/search").param("transportRouteId", "ROUTE-1");
            case 11 -> put("/api/v1/students/bulk-contact-update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(BulkContactUpdateRequest.builder()
                            .items(List.of(BulkContactUpdateRequest.ContactUpdateItem.builder()
                                    .studentId(STU_ID).emergencyContactPhone("+91-9876543210").build())).build()));
            case 12 -> post("/api/v1/students/" + STU_ID + "/activities")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LogActivityCreditRequest.builder()
                            .activityName("Debate").activityCategory("Literary").pointsAwarded(10).eventDate(LocalDate.now()).build()));
            case 13 -> post("/api/v1/alumni/credential-verifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SubmitCredentialVerificationRequest.builder()
                            .studentUid("STU2026000001").requestorName("HR Org").requestorOrganization("Infosys").credentialType("TC").build()));
            case 14 -> post("/api/v1/students/" + STU_ID + "/photo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(UpdatePhotoRequest.builder().photoUrl("https://img.kvs.local/p.jpg").build()));
            case 15 -> post("/api/v1/students/" + STU_ID + "/holds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PlaceHoldRequest.builder()
                            .holdType(HoldType.FEE_DEFAULT).reason("Fee overdue").build()));
            case 16 -> post("/api/v1/students/" + STU_ID + "/health-metrics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RecordHealthMetricRequest.builder()
                            .checkupDate(LocalDate.now()).heightCm(new BigDecimal("140.0")).weightKg(new BigDecimal("35.0")).build()));
            case 17 -> post("/api/v1/students/" + STU_ID + "/electives")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RecordElectiveSelectionRequest.builder()
                            .academicYearId(UUID.randomUUID()).firstLanguage("English").secondLanguage("Hindi").electiveSubjectCode("CS-101").build()));
            case 18 -> get("/api/v1/academic/sections/" + SEC_ID + "/roll-list");
            case 19 -> post("/api/v1/students/" + STU_ID + "/section-transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SectionTransferProposalRequest.builder()
                            .targetSectionId(SEC_ID).reason("Class balance").build()));
            default -> throw new IllegalArgumentException("Unknown capability: " + capIndex);
        };
    }
}
