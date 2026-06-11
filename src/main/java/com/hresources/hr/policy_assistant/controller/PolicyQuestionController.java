package com.hresources.hr.policy_assistant.controller;

import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import com.hresources.hr.policy_assistant.dto.PolicyQuestionRequest;
import com.hresources.hr.policy_assistant.service.PolicyAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyQuestionController {

    private final PolicyAssistantService policyAssistantService;

    public PolicyQuestionController(PolicyAssistantService policyAssistantService) {
        this.policyAssistantService = policyAssistantService;
    }

    @PostMapping("/ask")
    public PolicyAnswerResponse askPolicyQuestion(@Valid @RequestBody PolicyQuestionRequest request) {
        return policyAssistantService.answerQuestion(request.question());
    }
}
