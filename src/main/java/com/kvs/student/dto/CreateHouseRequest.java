package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHouseRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "houseName is required")
    private String houseName;

    private String colorHex;
}
