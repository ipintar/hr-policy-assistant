package com.hresources.hr.policy_assistant.service.retrieval;

import com.hresources.hr.policy_assistant.service.knowledge.PolicyDocument;

import java.util.List;

/**
 * Represents a retrieved policy document and its relevance score.
 *
 * @param document matched policy document
 * @param score retrieval score assigned to the document
 * @param matchedTerms normalized terms shared between the question and the document
 */
public record PolicyMatch(
        PolicyDocument document,
        double score,
        List<String> matchedTerms) {
}
