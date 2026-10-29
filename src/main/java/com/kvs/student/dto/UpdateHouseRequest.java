package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Renames a house or changes its colour (Settings → Academic setup → Classes, sections & houses). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHouseRequest {

    @NotBlank(message = "houseName is required")
    @Size(max = 50, message = "houseName cannot exceed 50 characters")
    private String houseName;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex must look like #1E88E5")
    private String colorHex;
}
