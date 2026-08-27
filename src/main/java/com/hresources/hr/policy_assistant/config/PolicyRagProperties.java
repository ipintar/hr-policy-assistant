package com.hresources.hr.policy_assistant.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties controlling local RAG chunking, retrieval, and prompting behavior.
 *
 * @param enabled whether RAG functionality is enabled
 * @param indexOnStartup whether the vector index should be rebuilt automatically during startup
 * @param chunkSize maximum number of characters stored in each chunk
 * @param chunkOverlap number of overlapping trailing characters carried into the next chunk
 * @param topK maximum number of retrieved chunks passed to the language model
 * @param chatModel OpenAI chat model used for answer generation
 * @param retrievalStrategy label describing the active retrieval implementation
 * @param systemPrompt system prompt used to constrain the assistant to retrieved context
 */
@Validated
@ConfigurationProperties(prefix = "policy.rag")
public record PolicyRagProperties(
        boolean enabled,
        boolean indexOnStartup,
        @Min(value = 100, message = "Chunk size must be at least 100 characters")
        int chunkSize,
        @Min(value = 0, message = "Chunk overlap must not be negative")
        int chunkOverlap,
        @Min(value = 1, message = "topK must be at least 1")
        int topK,
        @NotBlank(message = "Chat model must not be blank")
        String chatModel,
        @NotBlank(message = "Retrieval strategy must not be blank")
        String retrievalStrategy,
        @NotBlank(message = "System prompt must not be blank")
        String systemPrompt
) {
}
