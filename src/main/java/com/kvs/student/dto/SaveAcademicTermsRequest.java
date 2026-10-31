package com.kvs.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** "Manage term dates": the session's terms in order, replacing the ones saved before. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAcademicTermsRequest {

    @NotEmpty(message = "Add at least one term")
    @Size(max = 6, message = "A year has at most 6 terms")
    @Valid
    private List<Term> terms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Term {
        /** The saved term this row updates; blank for a new term */
        private java.util.UUID termId;

        @NotBlank(message = "Term name is required")
        @Size(max = 50)
        private String termName;

        @NotNull(message = "Term start date is required")
        private LocalDate startDate;

        @NotNull(message = "Term end date is required")
        private LocalDate endDate;

        private LocalDate halfTermStart;
        private LocalDate halfTermEnd;
        private Boolean isCurrent;

        public Term(String termName, LocalDate startDate, LocalDate endDate, LocalDate halfTermStart, LocalDate halfTermEnd, Boolean isCurrent) {
            this(null, termName, startDate, endDate, halfTermStart, halfTermEnd, isCurrent);
        }
    }
}
