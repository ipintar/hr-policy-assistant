package com.hresources.hr.policy_assistant.service.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Loads and exposes policy documents from the configured local knowledge base resource.
 */
@Component
public class PolicyKnowledgeBase {

    private final List<PolicyDocument> documents;

    /**
     * Creates a knowledge base by loading policy documents from a configured resource.
     *
     * @param objectMapper mapper used to deserialize the JSON knowledge base
     * @param knowledgeBaseResource resource containing policy documents
     */
    public PolicyKnowledgeBase(
            ObjectMapper objectMapper,
            @Value("${policy.knowledge-base.path}") Resource knowledgeBaseResource) {
        this.documents = loadDocuments(objectMapper, knowledgeBaseResource);
    }

    /**
     * Returns all policy documents loaded into memory.
     *
     * @return immutable view of loaded policy documents
     */
    public List<PolicyDocument> documents() {
        return documents;
    }

    /**
     * Loads policy documents from the provided resource.
     *
     * @param objectMapper mapper used to deserialize JSON content
     * @param knowledgeBaseResource resource containing the knowledge base file
     * @return deserialized policy documents
     */
    private List<PolicyDocument> loadDocuments(ObjectMapper objectMapper, Resource knowledgeBaseResource) {
        try (InputStream inputStream = knowledgeBaseResource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load policy knowledge base.", exception);
        }
    }
}
