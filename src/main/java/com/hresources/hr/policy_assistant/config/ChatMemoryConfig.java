package com.hresources.hr.policy_assistant.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures bounded, in-memory conversation history for the MVP.
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * Stores a bounded window of recent messages per conversation.
     *
     * @param ragProperties RAG and conversation settings
     * @return in-memory chat history implementation
     */
    @Bean
    public ChatMemory chatMemory(PolicyRagProperties ragProperties) {
        return MessageWindowChatMemory.builder()
                .maxMessages(ragProperties.chatMemorySize())
                .build();
    }
}
