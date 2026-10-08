package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload containing a single HR policy question.
 *
 * @param conversationId optional identifier used to continue a conversation
 * @param question question asked by the caller
 */
public record PolicyQuestionRequest(
        @Schema(
                description = "Optional conversation identifier. A new identifier is generated when omitted.",
                example = "8cc786e8-9843-4d1d-984d-f41f824bf46b"
        )
        @Size(max = 100, message = "Conversation ID must not exceed 100 characters")
        String conversationId,
        @Schema(
                description = "Question asked by the employee or HR user.",
                example = "How many vacation days do employees have?"
        )
        @NotBlank(message = "Question must not be blank")
        @Size(max = 2000, message = "Question must not exceed 2000 characters")
        String question) {
}
