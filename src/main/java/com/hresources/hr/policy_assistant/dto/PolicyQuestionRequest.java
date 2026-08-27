package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload containing a single HR policy question.
 *
 * @param question question asked by the caller
 */
public record PolicyQuestionRequest(
        @Schema(
                description = "Question asked by the employee or HR user.",
                example = "How many vacation days do employees have?"
        )
        @NotBlank(message = "Question must not be blank")
        @Size(max = 2000, message = "Question must not exceed 2000 characters")
        String question) {
}
