package com.hresources.hr.policy_assistant.service;

import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import com.hresources.hr.policy_assistant.dto.PolicyMatchResponse;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyMatch;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyRetriever;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates policy retrieval and converts matches into API responses.
 */
@Service
public class PolicyAssistantService {

    private static final String NO_MATCH_SOURCE = "knowledge-base-unmatched";
    private static final String RETRIEVAL_STRATEGY = "keyword-overlap";
    private static final int SUPPORTING_MATCH_LIMIT = 3;

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
        List<PolicyMatch> matches = policyRetriever.findTopMatches(question, SUPPORTING_MATCH_LIMIT);

        if (matches.isEmpty()) {
            return new PolicyAnswerResponse(
                    question,
                    "I could not find a matching HR policy yet. Please refine the question or expand the knowledge base.",
                    NO_MATCH_SOURCE,
                    0.0,
                    null,
                    RETRIEVAL_STRATEGY,
                    List.of()
            );
        }

        PolicyMatch topMatch = matches.get(0);

        return new PolicyAnswerResponse(
                question,
                topMatch.document().answer(),
                topMatch.document().source(),
                topMatch.score(),
                topMatch.document().id(),
                RETRIEVAL_STRATEGY,
                matches.stream()
                        .map(this::toMatchResponse)
                        .toList()
        );
    }

    /**
     * Converts an internal retrieval match into an API-facing supporting match response.
     *
     * @param match internal retrieval match
     * @return response DTO containing ranked match details
     */
    private PolicyMatchResponse toMatchResponse(PolicyMatch match) {
        return new PolicyMatchResponse(
                match.document().id(),
                match.document().title(),
                match.document().source(),
                match.document().answer(),
                match.score()
        );
    }
}
