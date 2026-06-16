package com.hresources.hr.policy_assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties controlling local RAG chunking, retrieval, and prompting behavior.
 *
 * @param chunkSize maximum number of characters stored in each chunk
 * @param chunkOverlap number of overlapping trailing characters carried into the next chunk
 * @param topK maximum number of retrieved chunks passed to the language model
 * @param chatModel OpenAI chat model used for answer generation
 * @param retrievalStrategy label describing the active retrieval implementation
 * @param systemPrompt system prompt used to constrain the assistant to retrieved context
 */
@ConfigurationProperties(prefix = "policy.rag")
public record PolicyRagProperties(
        int chunkSize,
        int chunkOverlap,
        int topK,
        String chatModel,
        String retrievalStrategy,
        String systemPrompt) {
}
