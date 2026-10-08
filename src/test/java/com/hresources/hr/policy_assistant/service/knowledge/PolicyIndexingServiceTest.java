package com.hresources.hr.policy_assistant.service.knowledge;

import com.hresources.hr.policy_assistant.config.PolicyRagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyIndexingServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void skipsEmbeddingWhenKnowledgeBaseAndSettingsAreUnchanged() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        VectorStore vectorStore = mock(VectorStore.class);
        Map<String, Integer> versionCounts = new HashMap<>();

        doReturn(List.of())
                .when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class));
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Integer.class), any()))
                .thenAnswer(invocation -> versionCounts.getOrDefault(invocation.getArgument(2), 0));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        doAnswer(invocation -> {
            List<Document> documents = invocation.getArgument(0);
            String version = documents.getFirst().getMetadata().get("indexVersion").toString();
            versionCounts.put(version, documents.size());
            return null;
        }).when(vectorStore).add(anyList());

        PolicyRagProperties properties = properties();
        PolicyIndexingService service = new PolicyIndexingService(
                jdbcTemplate,
                vectorStore,
                new PolicyKnowledgeBase("classpath*:policies/*.md"),
                new PolicyChunker(properties),
                properties
        );

        var firstStatus = service.refreshIndex();
        var secondStatus = service.refreshIndex();

        assertThat(firstStatus.indexed()).isTrue();
        assertThat(firstStatus.documentsLoaded()).isEqualTo(7);
        assertThat(firstStatus.chunksIndexed()).isEqualTo(7);
        assertThat(secondStatus.lastIndexedAt()).isEqualTo(firstStatus.lastIndexedAt());
        verify(vectorStore, times(1)).add(anyList());
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
