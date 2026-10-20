package com.kvs.student.dto;

import com.kvs.student.enums.FollowUpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Record the guardian follow-up of an unexcused absence. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFollowUpRequest {

    @NotNull(message = "Follow-up status is required")
    private FollowUpStatus followUpStatus;

    @NotBlank(message = "A note of what was done is required")
    @Size(max = 1000, message = "Note must be at most 1000 characters")
    private String note;
}
