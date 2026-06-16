package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Citation describing a retrieved policy chunk referenced by the generated answer.
 *
 * @param policyId identifier of the cited policy
 * @param title title of the cited policy
 * @param source source label of the cited policy
 * @param chunkIndex zero-based index of the cited chunk
 */
public record PolicyCitationResponse(
        @Schema(description = "Identifier of the cited policy.", example = "vacation-policy")
        String policyId,
        @Schema(description = "Title of the cited policy.", example = "Vacation Leave Policy")
        String title,
        @Schema(description = "Source label of the cited policy.", example = "hr-policy-handbook/vacation")
        String source,
        @Schema(description = "Zero-based index of the cited chunk.", example = "0")
        int chunkIndex) {
}
