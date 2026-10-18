package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkSiblingRequest {

    @NotBlank(message = "parentAccountId is required")
    private String parentAccountId;

    private UUID siblingStudentId;
}
