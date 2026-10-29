package com.kvs.student.importer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A bulk upload with its row results (rows are left out of the history list). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobResponse {
    private UUID jobId;
    private String fileName;
    private String branchId;
    private String status;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private int createdCount;
    /** CREATE, UPDATE or PHOTOS */
    private String mode;
    /** Records updated (UPDATE) or given a photo (PHOTOS) */
    private int updatedCount;
    private String createdBy;
    private Instant createdAt;
    private String committedBy;
    private Instant committedAt;
    private List<RowResult> rows;

    /** One data row: VALID, ERROR, CREATED, UPDATED, UNCHANGED (nothing to update) or FAILED (a valid row that could not be saved), with its errors and the new record. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowResult {
        private int rowNumber;
        private String status;
        /** A few values to recognise the row by (name, class), never personal identifiers */
        private Map<String, String> summary;
        @Builder.Default
        private List<RowError> errors = new ArrayList<>();
        private String recordId;
        private String recordCode;
        /** The row's values while the upload waits for Import (needed for the error report); cleared afterwards */
        private Map<String, String> values;
        /** Where a valid student row goes (set by the check, used by Import; not stored) */
        @JsonIgnore private UUID gradeId;
        @JsonIgnore private UUID sectionId;
        @JsonIgnore private UUID houseId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private String column;
        private String message;
    }
}
