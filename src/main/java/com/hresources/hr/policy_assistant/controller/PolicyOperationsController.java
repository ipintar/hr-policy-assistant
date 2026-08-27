package com.hresources.hr.policy_assistant.controller;

import com.hresources.hr.policy_assistant.dto.PolicyIndexStatusResponse;
import com.hresources.hr.policy_assistant.service.knowledge.PolicyIndexingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operational endpoints for policy indexing and RAG runtime status.
 */
@RestController
@RequestMapping("/api/policies")
@Tag(name = "Policy Operations", description = "Operational endpoints for policy indexing and vector-store status.")
public class PolicyOperationsController {

    private final PolicyIndexingService policyIndexingService;

    /**
     * Creates the operations controller backed by the indexing service.
     *
     * @param policyIndexingService service responsible for policy index lifecycle operations
     */
    public PolicyOperationsController(PolicyIndexingService policyIndexingService) {
        this.policyIndexingService = policyIndexingService;
    }

    /**
     * Returns the current policy indexing status.
     *
     * @return current indexing status for the vector store
     */
    @GetMapping("/index/status")
    @Operation(summary = "Get index status", description = "Returns the current status of policy indexing into the PGvector store.")
    public PolicyIndexStatusResponse getIndexStatus() {
        return policyIndexingService.getStatus();
    }

    /**
     * Rebuilds the policy vector index from the local knowledge base.
     *
     * @return updated indexing status after the rebuild completes
     */
    @PostMapping("/index/rebuild")
    @Operation(summary = "Rebuild policy index", description = "Rebuilds the PGvector index from the current local policy knowledge base.")
    public PolicyIndexStatusResponse rebuildIndex() {
        return policyIndexingService.rebuildIndex();
    }
}
