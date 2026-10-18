package com.kvs.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkContactUpdateRequest {

    @NotEmpty(message = "items list cannot be empty")
    @Valid
    private List<ContactUpdateItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactUpdateItem {
        @NotNull(message = "studentId is required")
        private UUID studentId;
        private String residentialAddress;
        private String emergencyContactPhone;
        private String guardianPhone;
        private String guardianEmail;
    }
}
