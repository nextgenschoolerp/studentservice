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
    @jakarta.validation.constraints.Size(max = 50, message = "houseName cannot exceed 50 characters")
    private String houseName;
    @jakarta.validation.constraints.Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex must look like #1E88E5")
    private String colorHex;
}
