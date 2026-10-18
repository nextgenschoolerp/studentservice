package com.kvs.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.*;
import com.kvs.student.enums.CertificateType;
import com.kvs.student.enums.IncidentSeverity;
import com.kvs.student.service.AlumniService;
import com.kvs.student.service.CertificateService;
import com.kvs.student.service.DisciplinaryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering user stories US-03005 through US-03009:
 * - US-03005: Annual batch promotions
 * - US-03006: Dual-signoff transfer certificate issuance
 * - US-03007: Bonafide / character certificates with QR verification
 * - US-03008: Disciplinary incident tracking & counseling logs
 * - US-03009: Permanent alumni registry archiving
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExtendedCapabilitiesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private CertificateService certificateService;

    @MockBean
    private DisciplinaryService disciplinaryService;

    @MockBean
    private AlumniService alumniService;

    private String principalToken;
    private String registrarToken;
    private String teacherToken;
    private String alumniCoordinatorToken;

    @BeforeEach
    void setUp() {
        principalToken = jwtTokenService.generateToken("principal@kvs.local", Map.of("roles", List.of("ROLE_SCHOOL_PRINCIPAL")));
        registrarToken = jwtTokenService.generateToken("registrar@kvs.local", Map.of("roles", List.of("ROLE_REGISTRAR")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.local", Map.of("roles", List.of("ROLE_CLASS_TEACHER")));
        alumniCoordinatorToken = jwtTokenService.generateToken("alumni@kvs.local", Map.of("roles", List.of("ROLE_ALUMNI_COORDINATOR")));
    }

    @Test
    @DisplayName("US-03005: POST /api/v1/academic/batch-promotion - Batch advance students returns 200")
    void testBatchPromotion_Success() throws Exception {
        UUID fromGradeId = UUID.randomUUID();
        UUID toGradeId = UUID.randomUUID();
        UUID currentSessionId = UUID.randomUUID();
        UUID nextSessionId = UUID.randomUUID();

        BatchPromotionRequest request = BatchPromotionRequest.builder()
                .currentAcademicYearId(currentSessionId)
                .fromGradeId(fromGradeId)
                .toGradeId(toGradeId)
                .nextAcademicYearId(nextSessionId)
                .build();

        BatchPromotionResponse response = BatchPromotionResponse.builder()
                .nextAcademicYearId(nextSessionId)
                .toGradeId(toGradeId)
                .totalPromoted(45)
                .status("SUCCESS")
                .build();

        when(studentService.batchPromoteStudents(any(BatchPromotionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/academic/batch-promotion")
                        .header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPromoted").value(45))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("US-03006: POST /api/v1/students/{studentId}/certificates/transfer - Dual sign-off TC returns 201")
    void testIssueTransferCertificate_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        IssueTransferCertificateRequest request = IssueTransferCertificateRequest.builder()
                .reasonForLeaving("Parent job relocation to Bangalore")
                .conductRemarks("Exemplary conduct and active sports participation")
                .secondSignOffUserId("principal@kvs.local")
                .build();

        CertificateResponse response = CertificateResponse.builder()
                .certificateId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000001")
                .studentName("Aarav Sharma")
                .certificateType(CertificateType.TRANSFER_CERTIFICATE)
                .certificateNumber("TC-2026-100001")
                .issueDate(LocalDate.now())
                .authorizedByUserId("registrar@kvs.local")
                .secondSignOffUserId("principal@kvs.local")
                .qrVerificationToken("TC-uuid-token")
                .verificationUrl("https://verify.kvs.local/certificates/TC-2026-100001")
                .build();

        when(certificateService.issueTransferCertificate(eq(studentId), any(IssueTransferCertificateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/certificates/transfer")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateNumber").value("TC-2026-100001"))
                .andExpect(jsonPath("$.data.secondSignOffUserId").value("principal@kvs.local"));
    }

    @Test
    @DisplayName("US-03007: POST /api/v1/students/{studentId}/certificates/bonafide - Issue Bonafide returns 201")
    void testIssueBonafideCertificate_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        IssueBonafideCertificateRequest request = IssueBonafideCertificateRequest.builder()
                .certificateType(CertificateType.BONAFIDE)
                .purpose("Passport Application")
                .build();

        CertificateResponse response = CertificateResponse.builder()
                .certificateId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000001")
                .studentName("Aarav Sharma")
                .certificateType(CertificateType.BONAFIDE)
                .certificateNumber("BON-2026-100002")
                .issueDate(LocalDate.now())
                .authorizedByUserId("registrar@kvs.local")
                .qrVerificationToken("BON-uuid-token")
                .verificationUrl("https://verify.kvs.local/certificates/BON-2026-100002")
                .build();

        when(certificateService.issueBonafideCertificate(eq(studentId), any(IssueBonafideCertificateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/certificates/bonafide")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateType").value("BONAFIDE"))
                .andExpect(jsonPath("$.data.certificateNumber").value("BON-2026-100002"));
    }

    @Test
    @DisplayName("US-03008: POST /api/v1/students/{studentId}/disciplinary - Record incident returns 201")
    void testRecordDisciplinaryIncident_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        RecordDisciplinaryIncidentRequest request = RecordDisciplinaryIncidentRequest.builder()
                .incidentDate(LocalDate.of(2026, 9, 10))
                .severity(IncidentSeverity.MODERATE)
                .incidentCategory("Repeated classroom disruption")
                .description("Disrupted physics lab session twice")
                .actionTaken("Written reflection submitted and parent counseling scheduled")
                .counselingNotes("Student advised on focus and team collaboration")
                .build();

        DisciplinaryIncidentResponse response = DisciplinaryIncidentResponse.builder()
                .incidentId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000001")
                .incidentDate(LocalDate.of(2026, 9, 10))
                .severity(IncidentSeverity.MODERATE)
                .incidentCategory("Repeated classroom disruption")
                .description("Disrupted physics lab session twice")
                .actionTaken("Written reflection submitted and parent counseling scheduled")
                .counselingNotes("Student advised on focus and team collaboration")
                .reportedByUserId("teacher@kvs.local")
                .build();

        when(disciplinaryService.recordIncident(eq(studentId), any(RecordDisciplinaryIncidentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/disciplinary")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.severity").value("MODERATE"))
                .andExpect(jsonPath("$.data.reportedByUserId").value("teacher@kvs.local"));
    }

    @Test
    @DisplayName("US-03009: POST /api/v1/alumni/archive - Archive graduating student into alumni ledger returns 201")
    void testArchiveAlumni_Success() throws Exception {
        UUID studentId = UUID.randomUUID();
        ArchiveAlumniRequest request = ArchiveAlumniRequest.builder()
                .studentId(studentId)
                .graduationYear(2026)
                .finalGradePassed("Grade 12 - Science")
                .alumniEmail("aarav.alumni@kvs.local")
                .alumniPhone("+919876543210")
                .higherEducationInstitution("Indian Institute of Technology, Bombay")
                .currentOccupation("Undergraduate Student - Computer Science")
                .build();

        AlumniResponse response = AlumniResponse.builder()
                .alumniId(UUID.randomUUID())
                .studentId(studentId)
                .studentUid("STU2026000001")
                .fullName("Aarav Sharma")
                .graduationYear(2026)
                .finalGradePassed("Grade 12 - Science")
                .alumniEmail("aarav.alumni@kvs.local")
                .alumniPhone("+919876543210")
                .higherEducationInstitution("Indian Institute of Technology, Bombay")
                .currentOccupation("Undergraduate Student - Computer Science")
                .archivedDate(LocalDate.now())
                .build();

        when(alumniService.archiveAlumni(any(ArchiveAlumniRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/alumni/archive")
                        .header("Authorization", "Bearer " + alumniCoordinatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.graduationYear").value(2026))
                .andExpect(jsonPath("$.data.higherEducationInstitution").value("Indian Institute of Technology, Bombay"));
    }

    @Test
    @DisplayName("US-03009: GET /api/v1/alumni/{studentUid} - Query alumni profile returns 200")
    void testGetAlumniByUid_Success() throws Exception {
        AlumniResponse response = AlumniResponse.builder()
                .alumniId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .studentUid("STU2026000001")
                .fullName("Aarav Sharma")
                .graduationYear(2026)
                .finalGradePassed("Grade 12 - Science")
                .higherEducationInstitution("Indian Institute of Technology, Bombay")
                .build();

        when(alumniService.getAlumniByUid("STU2026000001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/alumni/STU2026000001")
                        .header("Authorization", "Bearer " + principalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentUid").value("STU2026000001"))
                .andExpect(jsonPath("$.data.fullName").value("Aarav Sharma"));
    }
}
