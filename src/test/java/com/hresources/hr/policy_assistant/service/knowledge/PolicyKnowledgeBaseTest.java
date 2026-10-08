package com.hresources.hr.policy_assistant.service.knowledge;

import com.hresources.hr.policy_assistant.config.PolicyAssistantException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyKnowledgeBaseTest {

    @TempDir
    Path policyDirectory;

    @Test
    void loadsMarkdownPoliciesAndChangesChecksumWhenContentChanges() throws IOException {
        Path policyFile = policyDirectory.resolve("vacation.md");
        Files.writeString(policyFile, policyMarkdown("vacation", "Vacation Policy", "25 paid days."));
        PolicyKnowledgeBase knowledgeBase = new PolicyKnowledgeBase(resourcePattern());

        PolicyKnowledgeSnapshot firstSnapshot = knowledgeBase.loadSnapshot();

        assertThat(firstSnapshot.documents()).singleElement().satisfies(document -> {
            assertThat(document.id()).isEqualTo("vacation");
            assertThat(document.title()).isEqualTo("Vacation Policy");
            assertThat(document.source()).isEqualTo("handbook/vacation");
            assertThat(document.content()).isEqualTo("25 paid days.");
        });

        Files.writeString(policyFile, policyMarkdown("vacation", "Vacation Policy", "26 paid days."));
        PolicyKnowledgeSnapshot secondSnapshot = knowledgeBase.loadSnapshot();

        assertThat(secondSnapshot.checksum()).isNotEqualTo(firstSnapshot.checksum());
        assertThat(secondSnapshot.documents().getFirst().content()).isEqualTo("26 paid days.");
    }

    @Test
    void rejectsDuplicatePolicyIds() throws IOException {
        Files.writeString(
                policyDirectory.resolve("first.md"),
                policyMarkdown("duplicate", "First Policy", "First content.")
        );
        Files.writeString(
                policyDirectory.resolve("second.md"),
                policyMarkdown("duplicate", "Second Policy", "Second content.")
        );

        PolicyKnowledgeBase knowledgeBase = new PolicyKnowledgeBase(resourcePattern());

        assertThatThrownBy(knowledgeBase::loadSnapshot)
                .isInstanceOf(PolicyAssistantException.class)
                .hasMessageContaining("Duplicate policy ID");
    }

    private String resourcePattern() {
        return policyDirectory.toUri() + "*.md";
    }

    private String policyMarkdown(String id, String title, String content) {
        return """
                ---
                id: %s
                title: %s
                source: handbook/vacation
                ---

                %s
                """.formatted(id, title, content);
    }
}
