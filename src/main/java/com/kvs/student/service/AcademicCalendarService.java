package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.student.dto.AcademicCalendarResponse;
import com.kvs.student.dto.SaveAcademicTermsRequest;
import com.kvs.student.dto.UpdateAcademicSessionRequest;
import com.kvs.student.entity.AcademicSession;
import com.kvs.student.entity.AcademicTerm;
import com.kvs.student.repo.AcademicSessionRepository;
import com.kvs.student.repo.AcademicTermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Academic years and their terms (Settings → Academic setup, Calendar → Term dates). */
@Service
public class AcademicCalendarService {

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private AcademicTermRepository termRepository;

    @Autowired
    private StudentAuditService auditService;

    @Autowired
    private TenantAccessGuard tenantGuard;

    @Transactional(readOnly = true)
    public AcademicCalendarResponse calendar(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        List<AcademicSession> sessions = new ArrayList<>(sessionRepository.findByTenantId(tenantId));
        sessions.sort(Comparator.comparing(AcademicSession::getStartDate));
        return AcademicCalendarResponse.builder()
                .sessions(sessions)
                .terms(termRepository.findByTenantIdOrderByStartDateAsc(tenantId))
                .build();
    }

    @Transactional
    public AcademicSession updateSession(UUID academicYearId, UpdateAcademicSessionRequest request) {
        AcademicSession session = session(academicYearId);
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("The year must start before it ends");
        }
        // Years of a school may not overlap, and the year's terms must still fit inside its new dates
        for (AcademicSession other : sessionRepository.findByTenantId(session.getTenantId())) {
            if (!other.getAcademicYearId().equals(academicYearId)
                    && !request.getStartDate().isAfter(other.getEndDate()) && !request.getEndDate().isBefore(other.getStartDate())) {
                throw new BadRequestException(session.getSessionName() + " would overlap " + other.getSessionName()
                        + " (" + other.getStartDate() + " to " + other.getEndDate() + ")");
            }
        }
        for (AcademicTerm t : termRepository.findByAcademicYearIdOrderBySortOrderAsc(academicYearId)) {
            if (t.getStartDate().isBefore(request.getStartDate()) || t.getEndDate().isAfter(request.getEndDate())) {
                throw new BadRequestException(t.getTermName() + " (" + t.getStartDate() + " to " + t.getEndDate()
                        + ") would fall outside the year; change the term dates first");
            }
        }
        session.setStartDate(request.getStartDate());
        session.setEndDate(request.getEndDate());
        if (request.getWeekStructure() != null) {
            session.setWeekStructure(request.getWeekStructure());
        }
        if (Boolean.TRUE.equals(request.getIsActive()) && !Boolean.TRUE.equals(session.getIsActive())) {
            sessionRepository.findByTenantId(session.getTenantId()).forEach(s -> s.setIsActive(s.getAcademicYearId().equals(academicYearId)));
        }
        auditService.logSuccess("session:" + academicYearId, "UPDATE_SESSION", "Updated academic session " + session.getSessionName());
        return session;
    }

    /** Replaces the session's terms; terms may not overlap and a half-term must fall inside its term. */
    @Transactional
    public List<AcademicTerm> saveTerms(UUID academicYearId, SaveAcademicTermsRequest request) {
        AcademicSession session = session(academicYearId);
        List<SaveAcademicTermsRequest.Term> terms = new ArrayList<>(request.getTerms());
        terms.sort(Comparator.comparing(SaveAcademicTermsRequest.Term::getStartDate));
        long current = terms.stream().filter(t -> Boolean.TRUE.equals(t.getIsCurrent())).count();
        if (current > 1) {
            throw new BadRequestException("Only one term can be the current term");
        }
        for (int i = 0; i < terms.size(); i++) {
            SaveAcademicTermsRequest.Term t = terms.get(i);
            if (t.getStartDate().isAfter(t.getEndDate())) {
                throw new BadRequestException(t.getTermName() + " must start before it ends");
            }
            if ((t.getHalfTermStart() == null) != (t.getHalfTermEnd() == null)) {
                throw new BadRequestException(t.getTermName() + ": give both half-term dates or neither");
            }
            if (t.getHalfTermStart() != null && (t.getHalfTermStart().isAfter(t.getHalfTermEnd())
                    || t.getHalfTermStart().isBefore(t.getStartDate()) || t.getHalfTermEnd().isAfter(t.getEndDate()))) {
                throw new BadRequestException(t.getTermName() + ": the half-term must fall inside the term");
            }
            if (i > 0 && !t.getStartDate().isAfter(terms.get(i - 1).getEndDate())) {
                throw new BadRequestException(t.getTermName() + " overlaps " + terms.get(i - 1).getTermName());
            }
            if (t.getStartDate().isBefore(session.getStartDate()) || t.getEndDate().isAfter(session.getEndDate())) {
                throw new BadRequestException(t.getTermName() + " must fall inside " + session.getSessionName()
                        + " (" + session.getStartDate() + " to " + session.getEndDate() + ")");
            }
        }
        // Update the terms in place (their ids stay the same), add new ones, and remove the ones left out
        Map<UUID, AcademicTerm> existing = new HashMap<>();
        termRepository.findByAcademicYearIdOrderBySortOrderAsc(academicYearId).forEach(t -> existing.put(t.getTermId(), t));
        Set<UUID> kept = new HashSet<>();
        List<AcademicTerm> saved = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            SaveAcademicTermsRequest.Term t = terms.get(i);
            AcademicTerm row = t.getTermId() != null && existing.containsKey(t.getTermId()) ? existing.get(t.getTermId())
                    : AcademicTerm.builder().tenantId(session.getTenantId()).academicYearId(academicYearId).build();
            row.setTermName(t.getTermName().trim());
            row.setStartDate(t.getStartDate());
            row.setEndDate(t.getEndDate());
            row.setHalfTermStart(t.getHalfTermStart());
            row.setHalfTermEnd(t.getHalfTermEnd());
            row.setIsCurrent(Boolean.TRUE.equals(t.getIsCurrent()));
            row.setSortOrder(i + 1);
            if (row.getTermId() != null) kept.add(row.getTermId());
            saved.add(row);
        }
        existing.values().stream().filter(t -> !kept.contains(t.getTermId())).forEach(termRepository::delete);
        termRepository.flush();
        saved = termRepository.saveAll(saved);
        auditService.logSuccess("session:" + academicYearId, "SAVE_TERMS", "Saved " + saved.size() + " terms for " + session.getSessionName());
        return saved;
    }

    private AcademicSession session(UUID academicYearId) {
        AcademicSession session = sessionRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSession", "academicYearId", academicYearId));
        tenantGuard.requireTenant(session.getTenantId());
        return session;
    }
}
