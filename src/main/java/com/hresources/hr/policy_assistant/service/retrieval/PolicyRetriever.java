package com.hresources.hr.policy_assistant.service.retrieval;

/**
 * Defines the contract for finding the best policy match for a user question.
 */
public interface PolicyRetriever {

    /**
     * Finds the best matching policy for the supplied question.
     *
     * @param question user question to evaluate
     * @return best match or {@code null} when no document is suitable
     */
    PolicyMatch findBestMatch(String question);
}
