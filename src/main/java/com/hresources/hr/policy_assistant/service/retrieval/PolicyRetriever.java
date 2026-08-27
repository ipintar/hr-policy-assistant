package com.hresources.hr.policy_assistant.service.retrieval;

import java.util.List;

/**
 * Defines the contract for finding the best policy chunks for a user question.
 */
public interface PolicyRetriever {

    /**
     * Finds the highest-ranked policy matches for the supplied question.
     *
     * @param question user question to evaluate
     * @param limit maximum number of matches to return
     * @return ranked matches ordered from the best match to the weakest returned match
     */
    List<PolicyMatch> findTopMatches(String question, int limit);
}
