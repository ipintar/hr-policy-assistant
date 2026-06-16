package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ranked retrieval result exposed as supporting context in the API response.
 *
 * @param policyId identifier of the matched policy
 * @param title title of the matched policy
 * @param source source label of the matched policy
 * @param answer answer text associated with the matched policy
 * @param score retrieval score assigned to the matched policy
 */
public record PolicyMatchResponse(
        @Schema(description = "Identifier of the matched policy.", example = "remote-work-policy")
        String policyId,
        @Schema(description = "Title of the matched policy.", example = "Remote Work Policy")
        String title,
        @Schema(description = "Source label of the matched policy.", example = "hr-policy-handbook/remote-work")
        String source,
        @Schema(description = "Answer text associated with the matched policy.")
        String answer,
        @Schema(description = "Retrieval score assigned to the matched policy.", example = "0.66")
        double score
) {
}
