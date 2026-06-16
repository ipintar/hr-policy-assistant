package com.hresources.hr.policy_assistant.service;

import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyMatch;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyRetriever;
import org.springframework.stereotype.Service;

/**
 * Coordinates policy retrieval and converts matches into API responses.
 */
@Service
public class PolicyAssistantService {

    private static final String NO_MATCH_SOURCE = "knowledge-base-unmatched";

    private final PolicyRetriever policyRetriever;

    public PolicyAssistantService(PolicyRetriever policyRetriever) {
        this.policyRetriever = policyRetriever;
    }

    /**
     * Answers a user question using the configured local knowledge base.
     *
     * @param question policy-related question from the caller
     * @return matched policy answer or a fallback response when nothing matches
     */
    public PolicyAnswerResponse answerQuestion(String question) {
        PolicyMatch match = policyRetriever.findBestMatch(question);

        if (match == null) {
            return new PolicyAnswerResponse(
                    question,
                    "I could not find a matching HR policy yet. Please refine the question or expand the knowledge base.",
                    NO_MATCH_SOURCE,
                    0.0
            );
        }

        return new PolicyAnswerResponse(
                question,
                match.document().answer(),
                match.document().source(),
                match.score()
        );
    }
}
