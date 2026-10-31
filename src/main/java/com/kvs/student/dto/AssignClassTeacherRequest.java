package com.kvs.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Sets the class teacher (form tutor) of a section: an hrservice staff record. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignClassTeacherRequest {

    @NotNull(message = "staffId is required")
    private UUID staffId;

    @NotBlank(message = "staffName is required")
    @Size(max = 150, message = "staffName must be at most 150 characters")
    private String staffName;
}
