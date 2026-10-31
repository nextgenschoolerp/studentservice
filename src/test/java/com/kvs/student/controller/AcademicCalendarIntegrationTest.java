package com.kvs.student.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.service.JwtTokenService;
import com.kvs.student.dto.SaveAcademicTermsRequest;
import com.kvs.student.dto.UpdateAcademicSessionRequest;
import com.kvs.student.entity.AcademicSession;
import com.kvs.student.repo.AcademicSessionRepository;
import com.kvs.student.repo.AcademicTermRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Settings → Academic setup and Calendar → Term dates: academic years and their terms (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class AcademicCalendarIntegrationTest {

    private static final String TENANT = "TENANT-CAL";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private AcademicSessionRepository sessionRepository;
    @Autowired private AcademicTermRepository termRepository;

    private AcademicSession year;
    private AcademicSession nextYear;

    @BeforeEach
    void setUp() {
        year = sessionRepository.save(AcademicSession.builder().tenantId(TENANT).sessionName("2026-2027")
                .startDate(LocalDate.of(2026, 8, 31)).endDate(LocalDate.of(2027, 7, 23)).isActive(true).build());
        nextYear = sessionRepository.save(AcademicSession.builder().tenantId(TENANT).sessionName("2027-2028")
                .startDate(LocalDate.of(2027, 9, 1)).endDate(LocalDate.of(2028, 7, 21)).isActive(false).build());
    }

    @AfterEach
    void tearDown() {
        termRepository.deleteAll();
        sessionRepository.deleteAllById(List.of(year.getAcademicYearId(), nextYear.getAcademicYearId()));
    }

    private String token(String role, String tenantId) {
        return "Bearer " + jwtTokenService.generateToken(role.toLowerCase() + "@kvs.local",
                Map.of("roles", List.of("ROLE_" + role), "tenantId", tenantId));
    }

    private ResultActions putJson(String url, String token, Object body) throws Exception {
        return mockMvc.perform(put(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static SaveAcademicTermsRequest.Term term(String name, String start, String end, String halfStart, String halfEnd, boolean current) {
        return SaveAcademicTermsRequest.Term.builder()
                .termName(name)
                .startDate(LocalDate.parse(start))
                .endDate(LocalDate.parse(end))
                .halfTermStart(halfStart == null ? null : LocalDate.parse(halfStart))
                .halfTermEnd(halfEnd == null ? null : LocalDate.parse(halfEnd))
                .isCurrent(current)
                .build();
    }

    @Test
    @DisplayName("Terms: saved by the principal, read by a teacher, overlapping or misplaced half-terms rejected")
    void terms() throws Exception {
        String url = "/api/v1/academic/sessions/" + year.getAcademicYearId() + "/terms";
        SaveAcademicTermsRequest ok = new SaveAcademicTermsRequest(List.of(
                term("Term 1", "2026-08-31", "2026-12-18", "2026-10-26", "2026-10-30", true),
                term("Term 2", "2027-01-05", "2027-03-26", "2027-02-15", "2027-02-19", false)));
        putJson(url, token("TEACHER", TENANT), ok).andExpect(status().isForbidden());
        putJson(url, token("SCHOOL_PRINCIPAL", "TENANT-OTHER"), ok).andExpect(status().isForbidden());
        putJson(url, token("SCHOOL_PRINCIPAL", TENANT), ok).andExpect(status().isOk());

        SaveAcademicTermsRequest overlap = new SaveAcademicTermsRequest(List.of(
                term("Term 1", "2026-08-31", "2026-12-18", null, null, false),
                term("Term 2", "2026-12-01", "2027-03-26", null, null, false)));
        putJson(url, token("SCHOOL_PRINCIPAL", TENANT), overlap).andExpect(status().isBadRequest());
        SaveAcademicTermsRequest outside = new SaveAcademicTermsRequest(List.of(
                term("Term 1", "2026-08-31", "2026-12-18", "2026-12-20", "2026-12-24", false)));
        putJson(url, token("SCHOOL_PRINCIPAL", TENANT), outside).andExpect(status().isBadRequest());

        String body = mockMvc.perform(get("/api/v1/academic/calendar").param("tenantId", TENANT).header("Authorization", token("TEACHER", TENANT)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).get("data");
        assertThat(data.get("sessions").size()).isEqualTo(2);
        assertThat(data.get("terms").size()).isEqualTo(2);
        assertThat(data.get("terms").get(0).get("halfTermStart").asText()).isEqualTo("2026-10-26");
        assertThat(data.get("terms").get(0).get("isCurrent").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Session: dates, week structure and the current year are saved; end before start rejected")
    void session() throws Exception {
        String url = "/api/v1/academic/sessions/" + nextYear.getAcademicYearId();
        putJson(url, token("REGISTRAR", TENANT), new UpdateAcademicSessionRequest(
                LocalDate.of(2027, 9, 1), LocalDate.of(2027, 8, 1), null, null)).andExpect(status().isBadRequest());
        putJson(url, token("REGISTRAR", TENANT), new UpdateAcademicSessionRequest(
                LocalDate.of(2027, 9, 2), LocalDate.of(2028, 7, 20), true, "MON_SAT")).andExpect(status().isOk());

        assertThat(sessionRepository.findById(nextYear.getAcademicYearId()).orElseThrow().getWeekStructure()).isEqualTo("MON_SAT");
        assertThat(sessionRepository.findById(nextYear.getAcademicYearId()).orElseThrow().getIsActive()).isTrue();
        assertThat(sessionRepository.findById(year.getAcademicYearId()).orElseThrow().getIsActive()).isFalse();
    }
}
