package com.kvs.student.dto;

import com.kvs.student.enums.HoldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceHoldRequest {

    @NotNull(message = "holdType is required")
    private HoldType holdType;

    @NotBlank(message = "reason is required")
    private String reason;
}
