package com.hresources.hr.policy_assistant.service.retrieval;

import com.hresources.hr.policy_assistant.service.knowledge.PolicyDocument;
import com.hresources.hr.policy_assistant.service.knowledge.PolicyKnowledgeBase;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Retrieves policies by comparing normalized question tokens against document content and keywords.
 */
@Component
public class KeywordPolicyRetriever implements PolicyRetriever {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    private final PolicyKnowledgeBase policyKnowledgeBase;

    public KeywordPolicyRetriever(PolicyKnowledgeBase policyKnowledgeBase) {
        this.policyKnowledgeBase = policyKnowledgeBase;
    }

    /**
     * Finds the highest-scoring policy match for the given question.
     *
     * @param question user question to resolve
     * @return best matching document or {@code null} when no overlap exists
     */
    @Override
    public PolicyMatch findBestMatch(String question) {
        Set<String> questionTokens = tokenize(question);

        if (questionTokens.isEmpty()) {
            return null;
        }

        List<PolicyDocument> documents = policyKnowledgeBase.documents();

        return documents.stream()
                .map(document -> new PolicyMatch(document, calculateScore(questionTokens, document)))
                .filter(match -> match.score() > 0)
                .max(Comparator.comparingDouble(PolicyMatch::score))
                .orElse(null);
    }

    /**
     * Calculates a simple relevance score based on token overlap and explicit keyword hits.
     *
     * @param questionTokens normalized tokens from the input question
     * @param document candidate document being evaluated
     * @return score in the {@code [0, 1]} range
     */
    private double calculateScore(Set<String> questionTokens, PolicyDocument document) {
        Set<String> documentTokens = tokenize(document.title() + " " + document.content() + " " + String.join(" ", document.keywords()));
        long overlap = questionTokens.stream()
                .filter(documentTokens::contains)
                .count();

        if (overlap == 0) {
            return 0;
        }

        double keywordBonus = document.keywords().stream()
                .map(this::normalize)
                .filter(questionTokens::contains)
                .count() * 0.15;

        return Math.min(1.0, (double) overlap / questionTokens.size() + keywordBonus);
    }

    /**
     * Splits a string into normalized searchable tokens.
     *
     * @param value raw text to tokenize
     * @return set of normalized tokens with short values removed
     */
    private Set<String> tokenize(String value) {
        return TOKEN_SPLIT_PATTERN.splitAsStream(normalize(value))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    /**
     * Normalizes text to lowercase for case-insensitive comparisons.
     *
     * @param value raw text value
     * @return normalized lowercase text, or an empty string for {@code null}
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
