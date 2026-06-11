package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PolicyQuestionRequest(
        @Schema(
                description = "Question asked by the employee or HR user.",
                example = "How many vacation days do employees have?"
        )
        @NotBlank(message = "Question must not be blank")
        String question
) {
}
