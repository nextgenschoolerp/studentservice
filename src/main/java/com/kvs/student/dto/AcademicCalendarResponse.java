package com.kvs.student.dto;

import com.kvs.student.entity.AcademicSession;
import com.kvs.student.entity.AcademicTerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Every academic session of the tenant with its terms (Settings → Academic setup, Calendar → Term dates). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendarResponse {

    private List<AcademicSession> sessions;
    private List<AcademicTerm> terms;
}
