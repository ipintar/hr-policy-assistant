package com.hresources.hr.policy_assistant.dto;

import jakarta.validation.constraints.NotBlank;

public record PolicyQuestionRequest(
        @NotBlank(message = "Question must not be blank")
        String question
) {
}
