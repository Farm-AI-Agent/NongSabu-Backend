package com.nongsabu.backend.domain.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.dto.RagDiagnosticResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.infra.ai.llm.LlmClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private LlmClient llmClient;

    @Test
    void searchReturnsVectorStoreResultsAsRagItems() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("포도 노균병은 잎 뒷면을 확인합니다.", 10L, 0)));

        RagSearchResponse response = ragService.search(1L, "포도 노균병", 3);

        assertThat(response.query()).isEqualTo("포도 노균병");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).documentId()).isEqualTo(10L);
        assertThat(response.items().get(0).chunkIndex()).isZero();
        assertThat(response.items().get(0).content()).contains("포도 노균병");
    }

    @Test
    void askUsesLlmWhenEnabledAndSearchContextExists() {
        RagService ragService = ragService(true);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("포도 탄저병은 병든 잎과 과실을 제거합니다.", 11L, 2)));
        given(llmClient.generate(
                argThat(prompt -> prompt.contains("포도 탄저병 대처")),
                argThat(context -> context.contains("병든 잎과 과실"))
        )).willReturn("병든 잎과 과실을 제거하고 방제 이력을 기록하세요.");

        RagAnswerResponse response = ragService.ask(1L, "포도 탄저병 대처");

        assertThat(response.answer()).contains("방제 이력");
        verify(llmClient).generate(
                argThat(prompt -> prompt.contains("포도 탄저병 대처")),
                argThat(context -> context.contains("병든 잎과 과실"))
        );
    }

    @Test
    void askFallsBackToSearchEvidenceWhenLlmIsDisabled() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("포도 병해 의심 시 잎 뒷면과 과실 상태를 함께 확인합니다.", 12L, 1)));

        RagAnswerResponse response = ragService.ask(1L, "포도 병해 확인 방법");

        assertThat(response.answer()).contains("LLM 생성이 비활성화");
        assertThat(response.answer()).contains("잎 뒷면과 과실 상태");
        verify(llmClient, never()).generate(any(), any());
    }

    @Test
    void askReturnsGuideWhenNoSearchResultExists() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        RagAnswerResponse response = ragService.ask(1L, "지원하지 않는 질문");

        assertThat(response.answer()).contains("관련 근거를 찾지 못했습니다");
        verify(llmClient, never()).generate(any(), any());
    }

    @Test
    void diagnoseReturnsFailedStatusWhenVectorSearchFails() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willThrow(new IllegalStateException("embedding key missing"));

        RagDiagnosticResponse response = ragService.diagnose(1L);

        assertThat(response.ready()).isFalse();
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).contains("embedding key missing");
    }

    private RagService ragService(boolean llmEnabled) {
        return new RagService(vectorStore, llmClient, 3, 0.35, "test-embedding-model", llmEnabled);
    }

    private Document document(String content, Long documentId, int chunkIndex) {
        return new Document(
                content,
                Map.of(
                        "memberId", "1",
                        "documentId", String.valueOf(documentId),
                        "chunkIndex", chunkIndex,
                        "filename", "grape-manual.pdf"
                )
        );
    }
}
