package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionTransferProposalRequest {

    @NotNull(message = "targetSectionId is required")
    private UUID targetSectionId;

    @NotBlank(message = "reason is required")
    private String reason;
}
