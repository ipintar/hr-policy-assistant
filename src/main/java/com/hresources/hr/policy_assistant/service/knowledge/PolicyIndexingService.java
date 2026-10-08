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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates change-aware, versioned vector indexing operations.
 */
@Service
public class PolicyIndexingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyIndexingService.class);
    private static final String CREATE_STATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS policy_index_state (
                singleton_id integer PRIMARY KEY CHECK (singleton_id = 1),
                active_version varchar(64) NOT NULL,
                knowledge_checksum varchar(64) NOT NULL,
                documents_loaded integer NOT NULL,
                chunks_indexed integer NOT NULL,
                indexed_at timestamp with time zone NOT NULL
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final PolicyKnowledgeBase policyKnowledgeBase;
    private final PolicyChunker policyChunker;
    private final PolicyRagProperties ragProperties;
    private final AtomicBoolean stateTableInitialized = new AtomicBoolean();
    private final AtomicReference<IndexSnapshot> lastSnapshot = new AtomicReference<>();

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
    }

    /**
     * Rebuilds the index only when policy content or embedding-relevant settings changed.
     *
     * @return current indexing status
     */
    public synchronized PolicyIndexStatusResponse refreshIndex() {
        ensureEnabled();

        try {
            initializeStateTable();
            PolicyKnowledgeSnapshot knowledgeSnapshot = policyKnowledgeBase.loadSnapshot();
            String checksum = calculateIndexChecksum(knowledgeSnapshot);
            IndexSnapshot existing = currentSnapshot();

            if (existing != null
                    && checksum.equals(existing.knowledgeChecksum())
                    && fetchIndexedChunkCount(existing.activeVersion()) == existing.chunksIndexed()
                    && existing.chunksIndexed() > 0) {
                lastSnapshot.set(existing);
                LOGGER.info(
                        "Policy index is current at version {}. Skipping rebuild.",
                        existing.activeVersion()
                );
                return getStatus();
            }

            return rebuildSnapshot(knowledgeSnapshot, checksum);
        } catch (PolicyAssistantException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PolicyAssistantException("Failed to refresh the policy vector index.", exception);
        }
    }

    /**
     * Forces a new index version even when the knowledge-base checksum is unchanged.
     *
     * @return updated indexing status
     */
    public synchronized PolicyIndexStatusResponse rebuildIndex() {
        ensureEnabled();

        try {
            initializeStateTable();
            PolicyKnowledgeSnapshot knowledgeSnapshot = policyKnowledgeBase.loadSnapshot();
            return rebuildSnapshot(knowledgeSnapshot, calculateIndexChecksum(knowledgeSnapshot));
        } catch (PolicyAssistantException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PolicyAssistantException("Failed to rebuild the policy vector index.", exception);
        }
    }

    /**
     * Returns status persisted in PostgreSQL so it remains correct across restarts.
     *
     * @return current vector-index status
     */
    public PolicyIndexStatusResponse getStatus() {
        if (!ragProperties.enabled()) {
            return new PolicyIndexStatusResponse(false, false, 0, 0, null);
        }

        initializeStateTable();
        IndexSnapshot snapshot = currentSnapshot();

        if (snapshot == null) {
            return new PolicyIndexStatusResponse(
                    true,
                    false,
                    policyKnowledgeBase.loadSnapshot().documents().size(),
                    0,
                    null
            );
        }

        return new PolicyIndexStatusResponse(
                true,
                true,
                snapshot.documentsLoaded(),
                snapshot.chunksIndexed(),
                snapshot.lastIndexedAt()
        );
    }

    /**
     * Returns the index version that retrieval is allowed to query.
     *
     * @return active version or {@code null} when no index has completed
     */
    public String getActiveIndexVersion() {
        if (!ragProperties.enabled()) {
            return null;
        }
        initializeStateTable();
        IndexSnapshot snapshot = currentSnapshot();
        return snapshot == null ? null : snapshot.activeVersion();
    }

    public boolean isIndexOnStartupEnabled() {
        return ragProperties.indexOnStartup();
    }

    public boolean isRagEnabled() {
        return ragProperties.enabled();
    }

    private PolicyIndexStatusResponse rebuildSnapshot(
            PolicyKnowledgeSnapshot knowledgeSnapshot,
            String checksum) {
        List<PolicyChunk> chunks = knowledgeSnapshot.documents().stream()
                .flatMap(policyDocument -> policyChunker.chunk(policyDocument).stream())
                .toList();

        if (chunks.isEmpty()) {
            throw new PolicyAssistantException("No indexable policy content was found.");
        }

        String newVersion = UUID.randomUUID().toString();
        List<Document> vectorDocuments = chunks.stream()
                .map(chunk -> toVectorDocument(chunk, newVersion))
                .toList();

        // The previous version remains active while embeddings and rows are created.
        vectorStore.add(vectorDocuments);

        int indexedChunks = fetchIndexedChunkCount(newVersion);
        if (indexedChunks != vectorDocuments.size()) {
            cleanupVersion(newVersion);
            throw new PolicyAssistantException(
                    "The new policy index is incomplete: expected %d chunks but found %d."
                            .formatted(vectorDocuments.size(), indexedChunks)
            );
        }

        Instant indexedAt = Instant.now();
        persistActiveVersion(
                newVersion,
                checksum,
                knowledgeSnapshot.documents().size(),
                indexedChunks,
                indexedAt
        );

        IndexSnapshot snapshot = new IndexSnapshot(
                newVersion,
                checksum,
                knowledgeSnapshot.documents().size(),
                indexedChunks,
                indexedAt
        );
        lastSnapshot.set(snapshot);
        cleanupInactiveVersions(newVersion);

        LOGGER.info(
                "Activated policy index version {} with {} chunks from {} documents.",
                newVersion,
                indexedChunks,
                knowledgeSnapshot.documents().size()
        );
        return getStatus();
    }

    private Document toVectorDocument(PolicyChunk chunk, String indexVersion) {
        String documentId = UUID.nameUUIDFromBytes(
                (indexVersion + "|" + chunk.policyId() + "|" + chunk.chunkIndex())
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();

        return new Document(
                documentId,
                chunk.text(),
                Map.of(
                        "indexVersion", indexVersion,
                        "policyId", chunk.policyId(),
                        "title", chunk.title(),
                        "source", chunk.source(),
                        "chunkIndex", chunk.chunkIndex()
                )
        );
    }

    private String calculateIndexChecksum(PolicyKnowledgeSnapshot snapshot) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String fingerprint = "%s|chunkSize=%d|chunkOverlap=%d|embeddingModel=%s"
                    .formatted(
                            snapshot.checksum(),
                            ragProperties.chunkSize(),
                            ragProperties.chunkOverlap(),
                            ragProperties.embeddingModel()
                    );
            return HexFormat.of().formatHex(digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private void initializeStateTable() {
        if (stateTableInitialized.get()) {
            return;
        }

        synchronized (stateTableInitialized) {
            if (!stateTableInitialized.get()) {
                jdbcTemplate.execute(CREATE_STATE_TABLE_SQL);
                stateTableInitialized.set(true);
            }
        }
    }

    private IndexSnapshot currentSnapshot() {
        IndexSnapshot snapshot = lastSnapshot.get();
        if (snapshot != null) {
            return snapshot;
        }

        snapshot = readPersistedSnapshot();
        if (snapshot != null) {
            lastSnapshot.compareAndSet(null, snapshot);
        }
        return snapshot;
    }

    private IndexSnapshot readPersistedSnapshot() {
        return jdbcTemplate.query(
                        """
                        SELECT active_version, knowledge_checksum, documents_loaded, chunks_indexed, indexed_at
                        FROM policy_index_state
                        WHERE singleton_id = 1
                        """,
                        (resultSet, rowNumber) -> new IndexSnapshot(
                                resultSet.getString("active_version"),
                                resultSet.getString("knowledge_checksum"),
                                resultSet.getInt("documents_loaded"),
                                resultSet.getInt("chunks_indexed"),
                                resultSet.getTimestamp("indexed_at").toInstant()
                        )
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void persistActiveVersion(
            String version,
            String checksum,
            int documentsLoaded,
            int chunksIndexed,
            Instant indexedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO policy_index_state (
                    singleton_id,
                    active_version,
                    knowledge_checksum,
                    documents_loaded,
                    chunks_indexed,
                    indexed_at
                )
                VALUES (1, ?, ?, ?, ?, ?)
                ON CONFLICT (singleton_id) DO UPDATE SET
                    active_version = EXCLUDED.active_version,
                    knowledge_checksum = EXCLUDED.knowledge_checksum,
                    documents_loaded = EXCLUDED.documents_loaded,
                    chunks_indexed = EXCLUDED.chunks_indexed,
                    indexed_at = EXCLUDED.indexed_at
                """,
                version,
                checksum,
                documentsLoaded,
                chunksIndexed,
                Timestamp.from(indexedAt)
        );
    }

    private int fetchIndexedChunkCount(String version) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'indexVersion' = ?",
                Integer.class,
                version
        );
        return count == null ? 0 : count;
    }

    private void cleanupInactiveVersions(String activeVersion) {
        try {
            jdbcTemplate.update(
                    "DELETE FROM vector_store WHERE metadata->>'indexVersion' IS DISTINCT FROM ?",
                    activeVersion
            );
        } catch (Exception exception) {
            LOGGER.warn(
                    "Policy index {} is active, but old index rows could not be removed.",
                    activeVersion,
                    exception
            );
        }
    }

    private void cleanupVersion(String version) {
        try {
            jdbcTemplate.update(
                    "DELETE FROM vector_store WHERE metadata->>'indexVersion' = ?",
                    version
            );
        } catch (Exception exception) {
            LOGGER.warn("Could not remove incomplete policy index version {}.", version, exception);
        }
    }

    private void ensureEnabled() {
        if (!ragProperties.enabled()) {
            throw new PolicyAssistantException(
                    "RAG is disabled. Enable POLICY_RAG_ENABLED to use retrieval and indexing features."
            );
        }
    }

    private record IndexSnapshot(
            String activeVersion,
            String knowledgeChecksum,
            int documentsLoaded,
            int chunksIndexed,
            Instant lastIndexedAt
    ) {
    }
}
