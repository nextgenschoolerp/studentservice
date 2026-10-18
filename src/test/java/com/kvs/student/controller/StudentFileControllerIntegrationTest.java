package com.kvs.student.controller;

import com.kvs.core.service.JwtTokenService;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.Gender;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.repo.StudentDocumentRepository;
import com.kvs.student.repo.StudentMasterRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Student photo and document files end to end (H2 + a temporary vault folder):
 * Edit student → Change photo / Upload documents, the profile Documents tab and the download links.
 */
@SpringBootTest(properties = "student.file-vault.base-path=${java.io.tmpdir}/student-vault-test")
@AutoConfigureMockMvc
class StudentFileControllerIntegrationTest {

    private static final String TENANT = "TENANT-FILES";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};
    private static final byte[] PDF = "%PDF-1.4\n%%EOF".getBytes();

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private StudentMasterRepository studentRepository;
    @Autowired private StudentDocumentRepository documentRepository;

    private StudentMaster student;

    @BeforeEach
    void setUp() {
        student = studentRepository.save(StudentMaster.builder()
                .tenantId(TENANT).branchId("MAIN-01").studentUid("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .firstName("File").lastName("Student").dateOfBirth(LocalDate.of(2015, 3, 14)).gender(Gender.FEMALE)
                .studentStatus(StudentStatus.ACTIVE).build());
    }

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
        studentRepository.deleteAll();
    }

    private String token(String role, String tenantId) {
        return "Bearer " + jwtTokenService.generateToken(role.toLowerCase() + "@kvs.local",
                Map.of("roles", List.of("ROLE_" + role), "tenantId", tenantId));
    }

    @Test
    @DisplayName("Photo: registrar uploads a PNG, the record points to it and the file is served back")
    void photoUploadAndRead() throws Exception {
        mockMvc.perform(multipart("/api/v1/students/{id}/photo/upload", student.getStudentId())
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG))
                        .header("Authorization", token("REGISTRAR", TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profilePhotoUrl", startsWith("vault:")));

        mockMvc.perform(get("/api/v1/students/{id}/photo/file", student.getStudentId())
                        .header("Authorization", token("SCHOOL_NURSE", TENANT)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    @DisplayName("Photo: a PDF, a fake PNG, a class teacher and another tenant are refused")
    void photoRejected() throws Exception {
        mockMvc.perform(multipart("/api/v1/students/{id}/photo/upload", student.getStudentId())
                        .file(new MockMultipartFile("file", "photo.pdf", "application/pdf", PDF))
                        .header("Authorization", token("REGISTRAR", TENANT)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/students/{id}/photo/upload", student.getStudentId())
                        .file(new MockMultipartFile("file", "photo.png", "image/png", "not an image".getBytes()))
                        .header("Authorization", token("REGISTRAR", TENANT)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/students/{id}/photo/upload", student.getStudentId())
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG))
                        .header("Authorization", token("CLASS_TEACHER", TENANT)))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/v1/students/{id}/photo/upload", student.getStudentId())
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG))
                        .header("Authorization", token("REGISTRAR", "TENANT-OTHER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/students/{id}/photo/file", student.getStudentId())
                        .header("Authorization", token("REGISTRAR", TENANT)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Documents: upload a PDF, list it and download it; unknown type 400; other tenant 404")
    void documents() throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/students/{id}/documents", student.getStudentId())
                        .file(new MockMultipartFile("file", "birth.pdf", "application/pdf", PDF))
                        .param("documentType", "BIRTH_CERTIFICATE")
                        .header("Authorization", token("OFFICE_CLERK", TENANT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFileName").value("birth.pdf"))
                .andExpect(jsonPath("$.data.uploadedBy").value("office_clerk@kvs.local"))
                .andReturn().getResponse().getContentAsString();
        String documentId = body.replaceAll(".*\"documentId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/students/{id}/documents", student.getStudentId())
                        .header("Authorization", token("CLASS_TEACHER", TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentType").value("BIRTH_CERTIFICATE"))
                .andExpect(jsonPath("$.data[0].downloadUrl").value("/api/v1/students/documents/" + documentId + "/file"));

        mockMvc.perform(get("/api/v1/students/documents/{id}/file", documentId)
                        .header("Authorization", token("REGISTRAR", TENANT)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));

        mockMvc.perform(get("/api/v1/students/documents/{id}/file", documentId)
                        .header("Authorization", token("REGISTRAR", "TENANT-OTHER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(multipart("/api/v1/students/{id}/documents", student.getStudentId())
                        .file(new MockMultipartFile("file", "x.pdf", "application/pdf", PDF))
                        .param("documentType", "PASSPORT_PHOTO")
                        .header("Authorization", token("REGISTRAR", TENANT)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/v1/students/{id}/documents", student.getStudentId())
                        .file(new MockMultipartFile("file", "x.pdf", "application/pdf", PDF))
                        .param("documentType", "OTHER")
                        .header("Authorization", token("HOUSE_MASTER", TENANT)))
                .andExpect(status().isForbidden());
    }
}
