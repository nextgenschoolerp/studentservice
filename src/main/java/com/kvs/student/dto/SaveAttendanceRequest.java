package com.kvs.student.dto;

import com.kvs.student.enums.AttendanceSession;
import com.kvs.student.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Submit or correct one session (AM / PM) of a section's register. Students left out stay as they were. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAttendanceRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Session (AM or PM) is required")
    private AttendanceSession session;

    @NotEmpty(message = "At least one student mark is required")
    @Valid
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        @NotNull(message = "Student is required")
        private UUID studentId;

        @NotNull(message = "Status is required")
        private AttendanceStatus status;

        /** Required for EXCUSED */
        @Size(max = 500, message = "Reason must be at most 500 characters")
        private String reason;
    }
}
