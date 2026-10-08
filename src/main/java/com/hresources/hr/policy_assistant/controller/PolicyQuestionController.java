package com.hresources.hr.policy_assistant.controller;

import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import com.hresources.hr.policy_assistant.dto.PolicyQuestionRequest;
import com.hresources.hr.policy_assistant.service.PolicyAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handles incoming API requests for HR policy questions.
 */
@RestController
@RequestMapping("/api/policies")
@Tag(name = "Policies", description = "Endpoints for asking policy-related questions.")
public class PolicyQuestionController {

    private final PolicyAssistantService policyAssistantService;

    public PolicyQuestionController(PolicyAssistantService policyAssistantService) {
        this.policyAssistantService = policyAssistantService;
    }

    /**
     * Answers a policy question using vector retrieval and LLM generation.
     *
     * @param request question payload submitted by the client
     * @return RAG answer generated from retrieved policy chunks
     */
    @PostMapping("/ask")
    @Operation(
            summary = "Ask a policy question",
            description = "Accepts a policy question and returns a generated answer backed by retrieved policy chunks from the local vector knowledge base."
    )
    public PolicyAnswerResponse askPolicyQuestion(@Valid @RequestBody PolicyQuestionRequest request) {
        return policyAssistantService.answerQuestion(request.conversationId(), request.question());
    }

    /**
     * Clears all retained messages for a conversation.
     *
     * @param conversationId conversation to clear
     * @return confirmation payload
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Clear a conversation", description = "Removes the in-memory message history for a conversation.")
    public Map<String, String> clearConversation(@PathVariable String conversationId) {
        policyAssistantService.clearConversation(conversationId);
        return Map.of("conversationId", conversationId, "status", "cleared");
    }
}
