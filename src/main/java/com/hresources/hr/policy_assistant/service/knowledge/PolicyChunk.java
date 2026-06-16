package com.hresources.hr.policy_assistant.service.knowledge;

/**
 * Represents a chunked fragment of a policy document prepared for embedding and vector search.
 *
 * @param policyId identifier of the parent policy document
 * @param title title of the parent policy document
 * @param source source label of the parent policy document
 * @param chunkIndex zero-based chunk index inside the parent document
 * @param text chunk content stored in the vector database
 */
public record PolicyChunk(
        String policyId,
        String title,
        String source,
        int chunkIndex,
        String text) {
}
