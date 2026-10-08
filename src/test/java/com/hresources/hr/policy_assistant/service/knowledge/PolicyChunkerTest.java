package com.hresources.hr.policy_assistant.service.knowledge;

import com.hresources.hr.policy_assistant.config.PolicyRagProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyChunkerTest {

    @Test
    void createsOrderedOverlappingChunksWithoutSplittingAtAUsefulSpace() {
        PolicyChunker chunker = new PolicyChunker(properties(30, 5));
        PolicyDocument document = new PolicyDocument(
                "leave",
                "Leave Policy",
                "handbook/leave",
                "Employees receive annual leave and should arrange it with their manager in advance."
        );

        var chunks = chunker.chunk(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks)
                .extracting(PolicyChunk::chunkIndex)
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.range(0, chunks.size()).boxed().toList()
                );
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.policyId()).isEqualTo("leave");
            assertThat(chunk.text()).isNotBlank();
        });
    }

    @Test
    void ignoresBlankPolicyContent() {
        PolicyChunker chunker = new PolicyChunker(properties(100, 10));

        assertThat(chunker.chunk(new PolicyDocument("empty", "Empty", "test", "   "))).isEmpty();
    }

    private PolicyRagProperties properties(int chunkSize, int overlap) {
        return new PolicyRagProperties(
                true,
                true,
                chunkSize,
                overlap,
                4,
                10,
                "chat-model",
                "embedding-model",
                "test",
                "system prompt"
        );
    }
}
