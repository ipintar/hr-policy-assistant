package com.hresources.hr.policy_assistant.service.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Rebuilds the local policy vector index on application startup.
 */
@Component
public class PolicyVectorIndexer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyVectorIndexer.class);

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final PolicyKnowledgeBase policyKnowledgeBase;
    private final PolicyChunker policyChunker;

    /**
     * Creates the startup indexer responsible for chunking and indexing local policy documents.
     *
     * @param jdbcTemplate JDBC helper used to clear the vector table before reindexing
     * @param vectorStore vector store used to persist chunk embeddings
     * @param policyKnowledgeBase source policy documents
     * @param policyChunker chunker used to split policy text into indexable fragments
     */
    public PolicyVectorIndexer(
            JdbcTemplate jdbcTemplate,
            VectorStore vectorStore,
            PolicyKnowledgeBase policyKnowledgeBase,
            PolicyChunker policyChunker) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorStore = vectorStore;
        this.policyKnowledgeBase = policyKnowledgeBase;
        this.policyChunker = policyChunker;
    }

    /**
     * Rebuilds the vector index after the application context starts.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        List<Document> documents = policyKnowledgeBase.getDocuments().stream()
                .flatMap(policyDocument -> policyChunker.chunk(policyDocument).stream())
                .map(this::toVectorDocument)
                .toList();

        jdbcTemplate.execute("TRUNCATE TABLE vector_store");
        vectorStore.add(documents);

        LOGGER.info("Indexed {} policy chunks into PGvector.", documents.size());
    }

    /**
     * Converts a chunked policy fragment into a vector-store document with retrieval metadata.
     *
     * @param chunk policy chunk to index
     * @return vector-store document containing chunk text and metadata
     */
    private Document toVectorDocument(PolicyChunk chunk) {
        return new Document(
                chunk.text(),
                Map.of(
                        "policyId", chunk.policyId(),
                        "title", chunk.title(),
                        "source", chunk.source(),
                        "chunkIndex", chunk.chunkIndex()
                )
        );
    }
}
