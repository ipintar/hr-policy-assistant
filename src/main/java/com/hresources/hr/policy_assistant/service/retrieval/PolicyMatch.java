package com.hresources.hr.policy_assistant.service.retrieval;

import com.hresources.hr.policy_assistant.service.knowledge.PolicyChunk;

/**
 * Represents a retrieved policy chunk returned from the vector database.
 *
 * @param chunk matched policy chunk
 * @param rank zero-based rank of the chunk in retrieval results
 */
public record PolicyMatch(
        PolicyChunk chunk,
        int rank
) {
}
