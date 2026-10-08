package com.hresources.hr.policy_assistant.service;

import com.hresources.hr.policy_assistant.config.PolicyRagProperties;
import com.hresources.hr.policy_assistant.service.retrieval.PolicyRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyAssistantServiceTest {

    @Test
    void createsConversationAndUsesPreviousQuestionForFollowUpRetrieval() {
        List<String> retrievalQueries = new ArrayList<>();
        PolicyRetriever retriever = (question, limit) -> {
            retrievalQueries.add(question);
            return List.of();
        };
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));

        PolicyAssistantService service = new PolicyAssistantService(
                retriever,
                properties(),
                chatClientBuilder,
                chatMemory
        );

        var firstResponse = service.answerQuestion(null, "How many vacation days do employees have?");
        var secondResponse = service.answerQuestion(firstResponse.conversationId(), "Does that include contractors?");

        assertThat(firstResponse.conversationId()).isNotBlank();
        assertThat(secondResponse.conversationId()).isEqualTo(firstResponse.conversationId());
        assertThat(retrievalQueries.get(0)).isEqualTo("How many vacation days do employees have?");
        assertThat(retrievalQueries.get(1))
                .contains("How many vacation days do employees have?")
                .contains("Does that include contractors?");
        assertThat(chatMemory.get(firstResponse.conversationId())).hasSize(4);
    }

    @Test
    void clearsConversationHistory() {
        PolicyRetriever retriever = (question, limit) -> List.of();
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));
        PolicyAssistantService service = new PolicyAssistantService(
                retriever,
                properties(),
                chatClientBuilder,
                chatMemory
        );

        var response = service.answerQuestion("conversation-1", "What is the vacation policy?");
        service.clearConversation(response.conversationId());

        assertThat(chatMemory.get(response.conversationId())).isEmpty();
    }

    private PolicyRagProperties properties() {
        return new PolicyRagProperties(
                true,
                true,
                800,
                120,
                4,
                10,
                "chat-model",
                "embedding-model",
                "test",
                "system prompt"
        );
    }
}
