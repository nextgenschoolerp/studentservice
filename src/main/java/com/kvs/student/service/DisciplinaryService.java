package com.kvs.student.service;

import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.DisciplinaryIncidentResponse;
import com.kvs.student.dto.RecordDisciplinaryIncidentRequest;
import com.kvs.student.entity.DisciplinaryIncident;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.repo.DisciplinaryIncidentRepository;
import com.kvs.student.repo.StudentMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DisciplinaryService {

    @Autowired
    private DisciplinaryIncidentRepository incidentRepository;

    @Autowired
    private StudentMasterRepository studentRepository;

    @Autowired
    private StudentAuditService auditService;

    /**
     * US-03008: Track student disciplinary incidents and record counseling intervention logs
     */
    @Transactional
    public DisciplinaryIncidentResponse recordIncident(UUID studentId, RecordDisciplinaryIncidentRequest request) {
        log.info("Recording disciplinary incident for student: {}", studentId);
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        String reporter = SecurityUtils.getCurrentUserEmail().orElse("counselor");

        DisciplinaryIncident incident = DisciplinaryIncident.builder()
                .studentId(studentId)
                .incidentDate(request.getIncidentDate())
                .severity(request.getSeverity())
                .incidentCategory(request.getIncidentCategory())
                .description(request.getDescription())
                .actionTaken(request.getActionTaken())
                .counselingNotes(request.getCounselingNotes())
                .reportedByUserId(reporter)
                .build();

        DisciplinaryIncident saved = incidentRepository.save(incident);

        auditService.logSuccess("student:" + studentId, "RECORD_DISCIPLINARY_INCIDENT",
                "Recorded incident (" + request.getSeverity() + " - " + request.getIncidentCategory() + ") for student " + student.getStudentUid());

        return mapToResponse(saved, student);
    }

    @Transactional(readOnly = true)
    public List<DisciplinaryIncidentResponse> listIncidents(UUID studentId) {
        StudentMaster student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentMaster", "studentId", studentId));

        return incidentRepository.findByStudentIdOrderByIncidentDateDesc(studentId).stream()
                .map(i -> mapToResponse(i, student))
                .collect(Collectors.toList());
    }

    private DisciplinaryIncidentResponse mapToResponse(DisciplinaryIncident incident, StudentMaster student) {
        return DisciplinaryIncidentResponse.builder()
                .incidentId(incident.getIncidentId())
                .studentId(incident.getStudentId())
                .studentUid(student.getStudentUid())
                .incidentDate(incident.getIncidentDate())
                .severity(incident.getSeverity())
                .incidentCategory(incident.getIncidentCategory())
                .description(incident.getDescription())
                .actionTaken(incident.getActionTaken())
                .counselingNotes(incident.getCounselingNotes())
                .reportedByUserId(incident.getReportedByUserId())
                .build();
    }
}
