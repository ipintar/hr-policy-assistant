package com.hresources.hr.policy_assistant.service;

import com.hresources.hr.policy_assistant.dto.PolicyAnswerResponse;
import org.springframework.stereotype.Service;

@Service
public class PolicyAssistantService {

    public PolicyAnswerResponse answerQuestion(String question) {
        return new PolicyAnswerResponse(
                question,
                "This is a mock HR policy answer. The RAG integration will be added later.",
                "mock-policy-source",
                0.50
        );
    }
}
