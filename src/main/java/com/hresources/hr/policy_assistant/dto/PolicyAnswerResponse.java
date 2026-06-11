package com.hresources.hr.policy_assistant.dto;

public record PolicyAnswerResponse(
        String question,
        String answer,
        String source,
        double confidence
) {
}
