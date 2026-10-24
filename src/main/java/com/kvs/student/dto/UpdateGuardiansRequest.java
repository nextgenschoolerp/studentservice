package com.kvs.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** The student's guardians as they should stand after the save (PUT /students/{id}/guardians). Guardians not listed are removed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGuardiansRequest {

    @NotEmpty(message = "at least one guardian is required")
    @Valid
    private List<Item> guardians;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        /** Set for a guardian already on the record; null for a new one. */
        private UUID guardianId;

        @NotBlank(message = "fullName is required")
        private String fullName;

        @NotBlank(message = "relationship is required")
        private String relationship;

        /** Blank is allowed for a secondary guardian without a phone. */
        private String mobileNumber;

        private String email;

        @NotNull
        private Boolean primaryContact;
    }
}
