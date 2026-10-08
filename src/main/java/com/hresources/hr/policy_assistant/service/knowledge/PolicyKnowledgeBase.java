package com.hresources.hr.policy_assistant.service.knowledge;

import com.hresources.hr.policy_assistant.config.PolicyAssistantException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads Markdown policy documents from a configurable resource pattern.
 */
@Component
public class PolicyKnowledgeBase {

    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile(
            "\\A---\\R(.*?)\\R---\\R?(.*)\\z",
            Pattern.DOTALL
    );

    private final String knowledgeBasePath;
    private final ResourcePatternResolver resourceResolver;

    /**
     * Creates a reloadable knowledge base.
     *
     * @param knowledgeBasePath Spring resource pattern pointing to Markdown files
     */
    public PolicyKnowledgeBase(@Value("${policy.knowledge-base.path}") String knowledgeBasePath) {
        this(knowledgeBasePath, new PathMatchingResourcePatternResolver());
    }

    PolicyKnowledgeBase(String knowledgeBasePath, ResourcePatternResolver resourceResolver) {
        this.knowledgeBasePath = knowledgeBasePath;
        this.resourceResolver = resourceResolver;
    }

    /**
     * Reloads all documents and returns a stable content checksum.
     *
     * @return current knowledge-base snapshot
     */
    public PolicyKnowledgeSnapshot loadSnapshot() {
        try {
            List<PolicyDocument> documents = Arrays.stream(resourceResolver.getResources(knowledgeBasePath))
                    .filter(Resource::exists)
                    .sorted(Comparator.comparing(resource -> resource.getDescription().toLowerCase()))
                    .map(this::readDocument)
                    .toList();

            if (documents.isEmpty()) {
                throw new PolicyAssistantException(
                        "No policy Markdown documents were found at: " + knowledgeBasePath
                );
            }

            validateUniqueIds(documents);
            return new PolicyKnowledgeSnapshot(documents, calculateChecksum(documents));
        } catch (IOException exception) {
            throw new PolicyAssistantException(
                    "Unable to load policy documents from: " + knowledgeBasePath,
                    exception
            );
        }
    }

    /**
     * Returns the current documents, reloading the configured resources on every call.
     *
     * @return current policy documents
     */
    public List<PolicyDocument> getDocuments() {
        return loadSnapshot().documents();
    }

    private PolicyDocument readDocument(Resource resource) {
        try {
            String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
            Matcher matcher = FRONT_MATTER_PATTERN.matcher(markdown);

            if (!matcher.matches()) {
                throw invalidDocument(resource, "missing YAML-style front matter");
            }

            Map<String, String> metadata = parseMetadata(matcher.group(1), resource);
            String content = matcher.group(2).trim();

            if (content.isBlank()) {
                throw invalidDocument(resource, "policy content is empty");
            }

            return new PolicyDocument(
                    requiredMetadata(metadata, "id", resource),
                    requiredMetadata(metadata, "title", resource),
                    requiredMetadata(metadata, "source", resource),
                    content
            );
        } catch (IOException exception) {
            throw new PolicyAssistantException(
                    "Unable to read policy document: " + resource.getDescription(),
                    exception
            );
        }
    }

    private Map<String, String> parseMetadata(String frontMatter, Resource resource) {
        Map<String, String> metadata = new HashMap<>();

        frontMatter.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .forEach(line -> {
                    String[] parts = line.split(":", 2);
                    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                        throw invalidDocument(resource, "invalid metadata line: " + line);
                    }
                    metadata.put(parts[0].trim(), stripQuotes(parts[1].trim()));
                });

        return metadata;
    }

    private String requiredMetadata(Map<String, String> metadata, String key, Resource resource) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            throw invalidDocument(resource, "missing required metadata field: " + key);
        }
        return value;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private void validateUniqueIds(List<PolicyDocument> documents) {
        Set<String> ids = new HashSet<>();
        documents.forEach(document -> {
            if (!ids.add(document.id())) {
                throw new PolicyAssistantException("Duplicate policy ID: " + document.id());
            }
        });
    }

    private String calculateChecksum(List<PolicyDocument> documents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            documents.forEach(document -> {
                updateDigest(digest, document.id());
                updateDigest(digest, document.title());
                updateDigest(digest, document.source());
                updateDigest(digest, document.content());
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private PolicyAssistantException invalidDocument(Resource resource, String reason) {
        return new PolicyAssistantException(
                "Invalid policy document " + resource.getDescription() + ": " + reason
        );
    }
}
