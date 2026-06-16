package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response payload returned after policy retrieval completes.
 *
 * @param question original question submitted by the caller
 * @param answer primary answer selected from the knowledge base
 * @param source source label associated with the primary answer
 * @param confidence retrieval confidence score of the primary answer
 * @param matchedPolicyId identifier of the top-ranked policy document
 * @param retrievalStrategy retrieval approach used to produce the answer
 * @param supportingMatches ranked supporting matches returned from the knowledge base
 */
public record PolicyAnswerResponse(
        @Schema(description = "Original question submitted by the caller.")
        String question,
        @Schema(description = "Current answer returned by the policy assistant.")
        String answer,
        @Schema(description = "Source label backing the answer.")
        String source,
        @Schema(description = "Retrieval confidence score of the selected answer.", example = "0.72")
        double confidence,
        @Schema(description = "Identifier of the top-ranked policy document.", example = "vacation-policy")
        String matchedPolicyId,
        @Schema(description = "Name of the retrieval strategy used to answer the question.", example = "keyword-overlap")
        String retrievalStrategy,
        @Schema(description = "Top ranked supporting matches considered while answering the question.")
        List<PolicyMatchResponse> supportingMatches
) {
}
