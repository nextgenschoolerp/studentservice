package com.kvs.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.IssueBonafideCertificateRequest;
import com.kvs.student.dto.UpdateMedicalAlertsRequest;
import com.kvs.student.entity.AlumniRecord;
import com.kvs.student.entity.CertificateIssued;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.CertificateType;
import com.kvs.student.enums.Gender;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.repo.AlumniRecordRepository;
import com.kvs.student.repo.CertificateIssuedRepository;
import com.kvs.student.repo.StudentMasterRepository;
import com.kvs.student.service.UidGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end (real services, H2) checks that a caller only reaches students of the tenant in their token,
 * that the school nurse can list students, and that certificate numbers skip numbers already issued.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationIntegrationTest {

    private static final String OWN_TENANT = "TENANT-OWN";
    private static final String OTHER_TENANT = "TENANT-OTHER";
    private static final String BRANCH = "MAIN-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private StudentMasterRepository studentRepository;

    @Autowired
    private CertificateIssuedRepository certificateRepository;

    @Autowired
    private AlumniRecordRepository alumniRepository;

    private StudentMaster ownStudent;
    private StudentMaster otherStudent;
    private AlumniRecord otherAlumni;

    @BeforeEach
    void setUp() {
        ownStudent = studentRepository.save(student(OWN_TENANT, "Own"));
        otherStudent = studentRepository.save(student(OTHER_TENANT, "Other"));
        otherAlumni = alumniRepository.save(AlumniRecord.builder()
                .studentId(otherStudent.getStudentId())
                .tenantId(OTHER_TENANT)
                .branchId(BRANCH)
                .studentUid(otherStudent.getStudentUid())
                .fullName("Other Student")
                .graduationYear(2025)
                .finalGradePassed("Class 12")
                .archivedDate(LocalDate.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        certificateRepository.deleteAll();
        alumniRepository.deleteAll();
        studentRepository.deleteAll();
    }

    private static StudentMaster student(String tenantId, String firstName) {
        return StudentMaster.builder()
                .tenantId(tenantId)
                .branchId(BRANCH)
                .studentUid("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .firstName(firstName)
                .lastName("Student")
                .dateOfBirth(LocalDate.of(2014, 3, 9))
                .gender(Gender.FEMALE)
                .studentStatus(StudentStatus.ACTIVE)
                .build();
    }

    private String token(String role, String tenantId) {
        return "Bearer " + jwtTokenService.generateToken(role.toLowerCase() + "@kvs.local",
                Map.of("roles", List.of("ROLE_" + role), "tenantId", tenantId));
    }

    private String tokenWithoutTenant(String role) {
        return "Bearer " + jwtTokenService.generateToken(role.toLowerCase() + "@kvs.local",
                Map.of("roles", List.of("ROLE_" + role)));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} - own tenant's student returns 200, another tenant's returns 404")
    void testGetStudent_CrossTenantIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", ownStudent.getStudentId())
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Own"));

        mockMvc.perform(get("/api/v1/students/{id}", otherStudent.getStudentId())
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/students/uid/{uid}", otherStudent.getStudentUid())
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} - a token without a tenantId claim reaches no student")
    void testGetStudent_TokenWithoutTenantIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", ownStudent.getStudentId())
                        .header("Authorization", tokenWithoutTenant("REGISTRAR")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SUPER_ADMIN may read and list students of another tenant")
    void testSuperAdmin_CrossesTenants() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", otherStudent.getStudentId())
                        .header("Authorization", token("SUPER_ADMIN", OWN_TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Other"));

        mockMvc.perform(get("/api/v1/students")
                        .param("tenantId", OTHER_TENANT)
                        .param("branchId", BRANCH)
                        .header("Authorization", token("SUPER_ADMIN", OWN_TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("PUT/POST/GET per-student endpoints return 404 for another tenant's student")
    void testPerStudentEndpoints_CrossTenantIsNotFound() throws Exception {
        UUID otherId = otherStudent.getStudentId();

        UpdateMedicalAlertsRequest medical = UpdateMedicalAlertsRequest.builder()
                .bloodGroup("A+").medicalAlerts("None").emergencyContactPhone("+919800000000").build();
        mockMvc.perform(put("/api/v1/students/{id}/medical", otherId)
                        .header("Authorization", token("SCHOOL_NURSE", OWN_TENANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medical)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/students/{id}/id-card", otherId)
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/students/{id}/certificates", otherId)
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isNotFound());

        IssueBonafideCertificateRequest bonafide = IssueBonafideCertificateRequest.builder()
                .certificateType(CertificateType.BONAFIDE).purpose("Passport").build();
        mockMvc.perform(post("/api/v1/students/{id}/certificates/bonafide", otherId)
                        .header("Authorization", token("REGISTRAR", OWN_TENANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bonafide)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/students/{id}/disciplinary", otherId)
                        .header("Authorization", token("CLASS_TEACHER", OWN_TENANT)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/alumni/{uid}", otherAlumni.getStudentUid())
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/alumni/{uid}", otherAlumni.getStudentUid())
                        .header("Authorization", token("REGISTRAR", OTHER_TENANT)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/students?tenantId= - another tenant returns 403, own tenant returns 200")
    void testListStudents_OtherTenantIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .param("tenantId", OTHER_TENANT)
                        .param("branchId", BRANCH)
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/students")
                        .param("tenantId", OWN_TENANT)
                        .param("branchId", BRANCH)
                        .header("Authorization", token("REGISTRAR", OWN_TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].firstName").value("Own"));
    }

    @Test
    @DisplayName("GET /api/v1/students - the school nurse can list their own tenant's students")
    void testListStudents_NurseAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .param("tenantId", OWN_TENANT)
                        .param("branchId", BRANCH)
                        .header("Authorization", token("SCHOOL_NURSE", OWN_TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].firstName").value("Own"));

        mockMvc.perform(get("/api/v1/students")
                        .param("tenantId", OTHER_TENANT)
                        .param("branchId", BRANCH)
                        .header("Authorization", token("SCHOOL_NURSE", OWN_TENANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST bonafide - certificate numbers already issued (e.g. before a restart) are skipped")
    void testIssueCertificate_SkipsExistingNumbers() throws Exception {
        // Peek at the shared counter, then occupy the next three numbers as a previous run would have
        String peeked = UidGenerator.generateCertificateNumber("BON");
        String stem = peeked.substring(0, peeked.lastIndexOf('-') + 1);
        int counter = Integer.parseInt(peeked.substring(peeked.lastIndexOf('-') + 1));
        for (int i = 1; i <= 3; i++) {
            certificateRepository.save(CertificateIssued.builder()
                    .studentId(ownStudent.getStudentId())
                    .certificateType(CertificateType.BONAFIDE)
                    .certificateNumber(String.format("%s%05d", stem, counter + i))
                    .issueDate(LocalDate.now())
                    .authorizedByUserId("registrar@kvs.local")
                    .build());
        }

        IssueBonafideCertificateRequest request = IssueBonafideCertificateRequest.builder()
                .certificateType(CertificateType.BONAFIDE).purpose("Scholarship").build();
        mockMvc.perform(post("/api/v1/students/{id}/certificates/bonafide", ownStudent.getStudentId())
                        .header("Authorization", token("REGISTRAR", OWN_TENANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.certificateNumber").value(String.format("%s%05d", stem, counter + 4)));
    }
}
