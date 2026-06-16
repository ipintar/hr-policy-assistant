package com.hresources.hr.policy_assistant.service.retrieval;

import com.hresources.hr.policy_assistant.service.knowledge.PolicyChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Retrieves policy chunks from PGvector using embedding similarity search.
 */
@Component
public class VectorPolicyRetriever implements PolicyRetriever {

    private final VectorStore vectorStore;

    /**
     * Creates the retriever backed by a vector store.
     *
     * @param vectorStore vector store used for similarity search
     */
    public VectorPolicyRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Finds the highest-ranked policy chunks for the supplied question.
     *
     * @param question user question to evaluate
     * @param limit maximum number of matches to return
     * @return ranked vector-search matches
     */
    @Override
    public List<PolicyMatch> findTopMatches(String question, int limit) {
        if (question == null || question.isBlank() || limit <= 0) {
            return List.of();
        }

        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(limit)
                .build());

        return IntStream.range(0, results.size())
                .mapToObj(index -> toPolicyMatch(results.get(index), index))
                .toList();
    }

    /**
     * Converts a vector-store document back into a typed policy retrieval match.
     *
     * @param document retrieved vector-store document
     * @param rank zero-based rank in the result set
     * @return typed policy chunk match
     */
    private PolicyMatch toPolicyMatch(Document document, int rank) {
        Map<String, Object> metadata = document.getMetadata();

        PolicyChunk chunk = new PolicyChunk(
                stringValue(metadata, "policyId"),
                stringValue(metadata, "title"),
                stringValue(metadata, "source"),
                intValue(metadata, "chunkIndex"),
                document.getText()
        );

        return new PolicyMatch(chunk, rank);
    }

    /**
     * Reads a string metadata value from a retrieved document.
     *
     * @param metadata document metadata map
     * @param key metadata key to read
     * @return string representation of the metadata value
     */
    private String stringValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? "" : value.toString();
    }

    /**
     * Reads an integer metadata value from a retrieved document.
     *
     * @param metadata document metadata map
     * @param key metadata key to read
     * @return parsed integer value or zero when missing
     */
    private int intValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        return value == null ? 0 : Integer.parseInt(value.toString());
    }
}
