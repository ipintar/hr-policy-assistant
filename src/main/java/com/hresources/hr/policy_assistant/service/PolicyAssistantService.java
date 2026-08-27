package com.hresources.hr.policy_assistant.service;

import com.hresources.hr.policy_assistant.config.PolicyAssistantException;
import com.hresources.hr.policy_assistant.config.PolicyRagProperties;
import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import com.hresources.hr.policy_assistant.dto.PolicyCitationResponse;
import com.hresources.hr.policy_assistant.dto.PolicyMatchResponse;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyMatch;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates vector retrieval and answer generation for policy questions.
 */
@Service
public class PolicyAssistantService {

    private static final String FALLBACK_ANSWER = "I could not find enough policy context to answer that reliably. Please refine the question or expand the knowledge base.";

    private final PolicyRetriever policyRetriever;
    private final PolicyRagProperties ragProperties;
    private final ChatClient chatClient;

    /**
     * Creates the main assistant service with retrieval and chat-generation dependencies.
     *
     * @param policyRetriever retriever used to fetch relevant policy chunks
     * @param ragProperties RAG configuration settings
     * @param chatClientBuilder autoconfigured chat client builder for the OpenAI model
     */
    public PolicyAssistantService(
            PolicyRetriever policyRetriever,
            PolicyRagProperties ragProperties,
            ChatClient.Builder chatClientBuilder) {
        this.policyRetriever = policyRetriever;
        this.ragProperties = ragProperties;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Answers a user question using vector retrieval plus LLM generation.
     *
     * @param question policy-related question from the caller
     * @return RAG-generated answer with citations and retrieved chunks
     */
    public PolicyAnswerResponse answerQuestion(String question) {
        ensureEnabled();

        List<PolicyMatch> matches = policyRetriever.findTopMatches(question, ragProperties.topK());

        if (matches.isEmpty()) {
            return new PolicyAnswerResponse(
                    question,
                    FALLBACK_ANSWER,
                    ragProperties.chatModel(),
                    ragProperties.retrievalStrategy(),
                    List.of(),
                    List.of()
            );
        }

        try {
            String answer = chatClient.prompt()
                    .system(ragProperties.systemPrompt())
                    .user(buildUserPrompt(question, matches))
                    .call()
                    .content();

            return new PolicyAnswerResponse(
                    question,
                    answer,
                    ragProperties.chatModel(),
                    ragProperties.retrievalStrategy(),
                    matches.stream()
                            .map(this::toCitationResponse)
                            .distinct()
                            .toList(),
                    matches.stream()
                            .map(this::toChunkResponse)
                            .toList()
            );
        } catch (Exception exception) {
            throw new PolicyAssistantException("Failed to generate an answer from the retrieved policy context.", exception);
        }
    }

    /**
     * Ensures that RAG features are enabled before retrieval and generation work begins.
     */
    private void ensureEnabled() {
        if (!ragProperties.enabled()) {
            throw new PolicyAssistantException("RAG is disabled. Enable POLICY_RAG_ENABLED to answer policy questions.");
        }
    }

    /**
     * Builds the user prompt containing the question and retrieved chunk context.
     *
     * @param question user question to answer
     * @param matches retrieved policy chunks
     * @return user prompt text sent to the language model
     */
    private String buildUserPrompt(String question, List<PolicyMatch> matches) {
        String context = matches.stream()
                .map(match -> """
                        [%s#%d]
                        Title: %s
                        Source: %s
                        Content: %s
                        """.formatted(
                        match.chunk().policyId(),
                        match.chunk().chunkIndex(),
                        match.chunk().title(),
                        match.chunk().source(),
                        match.chunk().text()
                ))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");

        return """
                Question:
                %s

                Policy context:
                %s

                Return a concise answer grounded only in the policy context above.
                If the context is insufficient, clearly say so.
                Mention the relevant policy source names inside the answer when possible.
                """.formatted(question, context);
    }

    /**
     * Converts a retrieval match into a citation DTO.
     *
     * @param match retrieved policy chunk
     * @return citation describing the retrieved chunk
     */
    private PolicyCitationResponse toCitationResponse(PolicyMatch match) {
        return new PolicyCitationResponse(
                match.chunk().policyId(),
                match.chunk().title(),
                match.chunk().source(),
                match.chunk().chunkIndex()
        );
    }

    /**
     * Converts a retrieval match into an API-facing chunk response.
     *
     * @param match retrieved policy chunk
     * @return response DTO containing chunk context details
     */
    private PolicyMatchResponse toChunkResponse(PolicyMatch match) {
        return new PolicyMatchResponse(
                match.chunk().policyId(),
                match.chunk().title(),
                match.chunk().source(),
                match.chunk().chunkIndex(),
                match.chunk().text()
        );
    }
}
