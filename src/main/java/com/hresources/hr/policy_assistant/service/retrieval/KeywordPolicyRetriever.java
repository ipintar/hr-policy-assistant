package com.hresources.hr.policy_assistant.service.retrieval;

import com.hresources.hr.policy_assistant.service.knowledge.PolicyDocument;
import com.hresources.hr.policy_assistant.service.knowledge.PolicyKnowledgeBase;
import org.springframework.beans.factory.annotation.Value;
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
    private final int defaultMaxResults;
    private final double minimumScore;

    /**
     * Creates a keyword-based retriever backed by the local knowledge base.
     *
     * @param policyKnowledgeBase source of searchable policy documents
     * @param defaultMaxResults default number of matches returned for a search
     * @param minimumScore minimum score required for a match to be returned
     */
    public KeywordPolicyRetriever(
            PolicyKnowledgeBase policyKnowledgeBase,
            @Value("${policy.retrieval.max-results:3}") int defaultMaxResults,
            @Value("${policy.retrieval.min-score:0.20}") double minimumScore) {
        this.policyKnowledgeBase = policyKnowledgeBase;
        this.defaultMaxResults = defaultMaxResults;
        this.minimumScore = minimumScore;
    }

    /**
     * Finds the highest-scoring policy matches for the given question.
     *
     * @param question user question to resolve
     * @param limit maximum number of ranked matches to return
     * @return ranked list of matching documents
     */
    @Override
    public List<PolicyMatch> findTopMatches(String question, int limit) {
        Set<String> questionTokens = tokenize(question);

        if (questionTokens.isEmpty() || limit <= 0) {
            return List.of();
        }

        List<PolicyDocument> documents = policyKnowledgeBase.getDocuments();
        int effectiveLimit = Math.min(limit, defaultMaxResults);

        return documents.stream()
                .map(document -> buildMatch(questionTokens, document))
                .filter(match -> match.score() >= minimumScore)
                .sorted(Comparator.comparingDouble(PolicyMatch::score).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    /**
     * Builds a scored retrieval match for a candidate document.
     *
     * @param questionTokens normalized tokens from the input question
     * @param document candidate document being evaluated
     * @return retrieval match containing the score and matched terms
     */
    private PolicyMatch buildMatch(Set<String> questionTokens, PolicyDocument document) {
        Set<String> documentTokens = tokenize(document.title() + " " + document.content() + " " + String.join(" ", document.keywords()));
        List<String> matchedTerms = questionTokens.stream()
                .filter(documentTokens::contains)
                .sorted()
                .toList();

        long overlap = matchedTerms.size();

        if (overlap == 0) {
            return new PolicyMatch(document, 0.0, List.of());
        }

        double keywordBonus = document.keywords().stream()
                .map(this::normalize)
                .filter(questionTokens::contains)
                .count() * 0.15;

        double titleBonus = tokenize(document.title()).stream()
                .filter(questionTokens::contains)
                .count() * 0.10;

        double score = Math.min(1.0, (double) overlap / questionTokens.size() + keywordBonus + titleBonus);

        return new PolicyMatch(document, score, matchedTerms);
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
