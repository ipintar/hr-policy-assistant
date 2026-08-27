package com.hresources.hr.policy_assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Status payload describing the current state of policy vector indexing.
 *
 * @param ragEnabled whether RAG features are enabled
 * @param indexed whether at least one successful indexing run has completed
 * @param documentsLoaded number of policy documents loaded from the knowledge base
 * @param chunksIndexed number of chunks currently indexed into PGvector
 * @param lastIndexedAt time of the most recent successful indexing run
 */
public record PolicyIndexStatusResponse(
        @Schema(description = "Whether RAG features are enabled.", example = "true")
        boolean ragEnabled,
        @Schema(description = "Whether at least one successful indexing run has completed.", example = "true")
        boolean indexed,
        @Schema(description = "Number of policy documents loaded from the source knowledge base.", example = "7")
        int documentsLoaded,
        @Schema(description = "Number of chunks indexed into the vector store.", example = "14")
        int chunksIndexed,
        @Schema(description = "Timestamp of the last successful indexing run.")
        Instant lastIndexedAt
) {
}
