package com.hresources.hr.policy_assistant.controller;

import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import com.hresources.hr.policy_assistant.dto.PolicyQuestionRequest;
import com.hresources.hr.policy_assistant.service.PolicyAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@Tag(name = "Policies", description = "Endpoints for asking policy-related questions.")
public class PolicyQuestionController {

    private final PolicyAssistantService policyAssistantService;

    public PolicyQuestionController(PolicyAssistantService policyAssistantService) {
        this.policyAssistantService = policyAssistantService;
    }

    @PostMapping("/ask")
    @Operation(
            summary = "Ask a policy question",
            description = "Accepts a policy question and returns a mock answer for the current MVP stage."
    )
    public PolicyAnswerResponse askPolicyQuestion(@Valid @RequestBody PolicyQuestionRequest request) {
        return policyAssistantService.answerQuestion(request.question());
    }
}
