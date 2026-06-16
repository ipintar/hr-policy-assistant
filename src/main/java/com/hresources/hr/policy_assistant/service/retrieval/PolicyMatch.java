package com.hresources.hr.policy_assistant.service.retrieval;

import com.hresources.hr.policy_assistant.service.knowledge.PolicyDocument;

/**
 * Represents a retrieved policy document and its relevance score.
 *
 * @param document matched policy document
 * @param score retrieval score assigned to the document
 */
public record PolicyMatch(
        PolicyDocument document,
        double score) {
}
