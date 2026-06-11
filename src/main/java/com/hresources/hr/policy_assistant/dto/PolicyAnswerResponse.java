package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PolicyAnswerResponse(
        @Schema(description = "Original question submitted by the caller.")
        String question,
        @Schema(description = "Current answer returned by the policy assistant.")
        String answer,
        @Schema(description = "Source label backing the answer.")
        String source,
        @Schema(description = "Temporary mock confidence score used in the MVP response.", example = "0.5")
        double confidence
) {
}
