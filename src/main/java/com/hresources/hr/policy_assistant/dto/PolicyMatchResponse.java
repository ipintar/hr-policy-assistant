package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ranked chunk retrieved from the vector store and supplied to the LLM as context.
 *
 * @param policyId identifier of the matched policy
 * @param title title of the matched policy
 * @param source source label of the matched policy
 * @param chunkIndex zero-based chunk index within the source policy
 * @param excerpt chunk text that was retrieved for the question
 */
public record PolicyMatchResponse(
        @Schema(description = "Identifier of the matched policy.", example = "remote-work-policy")
        String policyId,
        @Schema(description = "Title of the matched policy.", example = "Remote Work Policy")
        String title,
        @Schema(description = "Source label of the matched policy.", example = "hr-policy-handbook/remote-work")
        String source,
        @Schema(description = "Zero-based index of the retrieved chunk.", example = "0")
        int chunkIndex,
        @Schema(description = "Retrieved chunk text that was supplied to the language model.")
        String excerpt
) {
}
