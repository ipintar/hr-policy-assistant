package com.hresources.hr.policy_assistant.service.knowledge;

import com.hresources.hr.policy_assistant.config.PolicyRagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits policy documents into overlapping text chunks suitable for embedding and retrieval.
 */
@Component
public class PolicyChunker {

    private final PolicyRagProperties ragProperties;

    /**
     * Creates the chunker with configurable chunk sizing rules.
     *
     * @param ragProperties chunking configuration properties
     */
    public PolicyChunker(PolicyRagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * Splits a policy document into overlapping chunks.
     *
     * @param document source policy document
     * @return ordered list of chunks derived from the document content
     */
    public List<PolicyChunk> chunk(PolicyDocument document) {
        String normalizedContent = normalize(document.content());

        if (normalizedContent.isBlank()) {
            return List.of();
        }

        List<PolicyChunk> chunks = new ArrayList<>();
        int start = 0;
        int chunkIndex = 0;

        while (start < normalizedContent.length()) {
            int end = Math.min(start + ragProperties.chunkSize(), normalizedContent.length());

            if (end < normalizedContent.length()) {
                int lastSpace = normalizedContent.lastIndexOf(' ', end);

                if (lastSpace > start + (ragProperties.chunkSize() / 2)) {
                    end = lastSpace;
                }
            }

            String chunkText = normalizedContent.substring(start, end).trim();

            if (!chunkText.isEmpty()) {
                chunks.add(new PolicyChunk(
                        document.id(),
                        document.title(),
                        document.source(),
                        chunkIndex,
                        chunkText
                ));
                chunkIndex++;
            }

            if (end >= normalizedContent.length()) {
                break;
            }

            start = Math.max(end - ragProperties.chunkOverlap(), start + 1);

            while (start < normalizedContent.length() && normalizedContent.charAt(start) == ' ') {
                start++;
            }
        }

        return chunks;
    }

    /**
     * Normalizes policy content into a single searchable text block.
     *
     * @param content raw policy content
     * @return whitespace-normalized text
     */
    private String normalize(String content) {
        return content == null ? "" : content.replaceAll("\\s+", " ").trim();
    }
}
