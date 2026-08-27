package com.hresources.hr.policy_assistant.service.knowledge;

import com.hresources.hr.policy_assistant.config.PolicyAssistantException;
import com.hresources.hr.policy_assistant.config.PolicyRagProperties;
import com.hresources.hr.policy_assistant.dto.PolicyIndexStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates vector indexing operations and exposes indexing status for operations endpoints.
 */
@Service
public class PolicyIndexingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyIndexingService.class);

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final PolicyKnowledgeBase policyKnowledgeBase;
    private final PolicyChunker policyChunker;
    private final PolicyRagProperties ragProperties;
    private final AtomicReference<IndexSnapshot> lastSnapshot;

    /**
     * Creates the indexing service with knowledge-base, chunking, and vector-store dependencies.
     *
     * @param jdbcTemplate JDBC helper used for vector table maintenance
     * @param vectorStore vector store used to persist chunk embeddings
     * @param policyKnowledgeBase source policy documents
     * @param policyChunker chunker used to split policies into indexable fragments
     * @param ragProperties RAG configuration flags and limits
     */
    public PolicyIndexingService(
            JdbcTemplate jdbcTemplate,
            VectorStore vectorStore,
            PolicyKnowledgeBase policyKnowledgeBase,
            PolicyChunker policyChunker,
            PolicyRagProperties ragProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorStore = vectorStore;
        this.policyKnowledgeBase = policyKnowledgeBase;
        this.policyChunker = policyChunker;
        this.ragProperties = ragProperties;
        this.lastSnapshot = new AtomicReference<>(new IndexSnapshot(false, policyKnowledgeBase.getDocuments().size(), 0, null));
    }

    /**
     * Rebuilds the vector index from the current local knowledge base.
     *
     * @return updated indexing status after the rebuild completes
     */
    public PolicyIndexStatusResponse rebuildIndex() {
        ensureEnabled();

        try {
            List<Document> documents = policyKnowledgeBase.getDocuments().stream()
                    .flatMap(policyDocument -> policyChunker.chunk(policyDocument).stream())
                    .map(this::toVectorDocument)
                    .toList();

            jdbcTemplate.execute("TRUNCATE TABLE vector_store");
            vectorStore.add(documents);

            int chunksIndexed = fetchIndexedChunkCount();
            Instant now = Instant.now();
            lastSnapshot.set(new IndexSnapshot(true, policyKnowledgeBase.getDocuments().size(), chunksIndexed, now));

            LOGGER.info("Indexed {} chunks from {} policy documents.", chunksIndexed, policyKnowledgeBase.getDocuments().size());
            return getStatus();
        } catch (Exception exception) {
            throw new PolicyAssistantException("Failed to rebuild the policy vector index.", exception);
        }
    }

    /**
     * Returns the current vector-index status snapshot.
     *
     * @return indexing status response for operational inspection
     */
    public PolicyIndexStatusResponse getStatus() {
        IndexSnapshot snapshot = lastSnapshot.get();
        return new PolicyIndexStatusResponse(
                ragProperties.enabled(),
                snapshot.indexed(),
                snapshot.documentsLoaded(),
                snapshot.chunksIndexed(),
                snapshot.lastIndexedAt()
        );
    }

    /**
     * Returns whether indexing should run automatically during application startup.
     *
     * @return {@code true} when startup indexing is enabled
     */
    public boolean isIndexOnStartupEnabled() {
        return ragProperties.indexOnStartup();
    }

    /**
     * Ensures that RAG features are currently enabled before indexing work begins.
     */
    private void ensureEnabled() {
        if (!ragProperties.enabled()) {
            throw new PolicyAssistantException("RAG is disabled. Enable POLICY_RAG_ENABLED to use retrieval and indexing features.");
        }
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

    /**
     * Reads the current chunk count from the PGvector backing table.
     *
     * @return number of indexed chunk rows
     */
    private int fetchIndexedChunkCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Immutable in-memory snapshot of the last indexing outcome.
     *
     * @param indexed whether indexing has successfully completed
     * @param documentsLoaded number of source documents loaded
     * @param chunksIndexed number of indexed chunks
     * @param lastIndexedAt timestamp of the last successful index build
     */
    private record IndexSnapshot(
            boolean indexed,
            int documentsLoaded,
            int chunksIndexed,
            Instant lastIndexedAt
    ) {
    }
}
