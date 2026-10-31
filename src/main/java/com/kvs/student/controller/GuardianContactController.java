package com.kvs.student.controller;

import com.kvs.core.dto.ApiResponse;
import com.kvs.student.service.GuardianContactService;
import com.kvs.student.service.GuardianContactService.GuardianContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Families to send a circular to (communicationservice, Communications → Circulars): one primary guardian per student on roll,
 * for the whole school or one class. Readable by those who may send circulars.
 */
@RestController
@RequestMapping("/api/v1/students/guardian-contacts")
public class GuardianContactController {

    private static final String SEND = "hasAnyRole('PUBLIC_RELATIONS_OFFICER', 'COMMUNICATION_OFFICER', 'PRINCIPAL', 'SCHOOL_PRINCIPAL', 'SUPER_ADMIN', 'ADMIN') or hasAnyAuthority('ROLE_PUBLIC_RELATIONS_OFFICER', 'ROLE_COMMUNICATION_OFFICER', 'ROLE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SUPER_ADMIN', 'ROLE_ADMIN')";

    @Autowired
    private GuardianContactService service;

    /** gradeCode: e.g. GRADE-10; omit for every class */
    @GetMapping
    @PreAuthorize(SEND)
    public ResponseEntity<ApiResponse<List<GuardianContact>>> contacts(@RequestParam("tenantId") String tenantId,
                                                                      @RequestParam(value = "gradeCode", required = false) String gradeCode) {
        return ResponseEntity.ok(ApiResponse.success(service.contacts(tenantId, gradeCode)));
    }
}
