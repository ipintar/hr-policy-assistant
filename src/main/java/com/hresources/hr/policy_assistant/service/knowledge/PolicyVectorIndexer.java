package com.hresources.hr.policy_assistant.service.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Rebuilds the local policy vector index on application startup when enabled.
 */
@Component
public class PolicyVectorIndexer implements ApplicationRunner, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyVectorIndexer.class);

    private final PolicyIndexingService policyIndexingService;

    /**
     * Creates the startup indexer responsible for delegating vector-index rebuilds.
     *
     * @param policyIndexingService service responsible for index rebuilds and status tracking
     */
    public PolicyVectorIndexer(PolicyIndexingService policyIndexingService) {
        this.policyIndexingService = policyIndexingService;
    }

    /**
     * Rebuilds the vector index after the application context starts when startup indexing is enabled.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!policyIndexingService.getStatus().ragEnabled()) {
            LOGGER.info("Skipping startup policy indexing because RAG is disabled.");
            return;
        }

        if (!policyIndexingService.isIndexOnStartupEnabled()) {
            LOGGER.info("Skipping startup policy indexing because POLICY_RAG_INDEX_ON_STARTUP is false.");
            return;
        }

        LOGGER.info("Building policy vector index on startup.");
        policyIndexingService.rebuildIndex();
    }

    /**
     * Returns the execution order for the startup indexer.
     *
     * @return startup order for this application runner
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
