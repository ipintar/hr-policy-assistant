package com.hresources.hr.policy_assistant.service.knowledge;

import java.util.List;

/**
 * Immutable view of the currently loaded policy documents and their checksum.
 *
 * @param documents policy documents loaded from the configured source
 * @param checksum stable SHA-256 checksum of policy metadata and content
 */
public record PolicyKnowledgeSnapshot(
        List<PolicyDocument> documents,
        String checksum
) {
    public PolicyKnowledgeSnapshot {
        documents = List.copyOf(documents);
    }
}
