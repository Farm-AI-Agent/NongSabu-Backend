package com.nongsabu.backend.domain.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.dto.RagDiagnosticResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.infra.reranker.RerankerClient;
import com.nongsabu.backend.infra.search.opensearch.Bm25SearchClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private Bm25SearchClient bm25SearchClient;

    @Mock
    private RerankerClient rerankerClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    void searchReturnsVectorStoreResultsAsRagItems() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("포도 탄저병은 잎 뒷면을 확인합니다.", 10L, 0)));

        RagSearchResponse response = ragService.search(1L, "포도 탄저병", 3);

        assertThat(response.query()).isEqualTo("포도 탄저병");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).documentId()).isEqualTo(10L);
        assertThat(response.items().get(0).chunkIndex()).isZero();
        assertThat(response.items().get(0).content()).contains("포도 탄저병");
        assertThat(response.items().get(0).retrievalSource()).isEqualTo("dense");
    }

    @Test
    void searchCanUseBm25Mode() {
        RagService ragService = ragService(false);
        given(bm25SearchClient.search(1L, "포도 병해", 2))
                .willReturn(List.of(searchItem("bm25-1", 10L, 1, "포도 병해 BM25 결과", "bm25")));

        RagSearchResponse response = ragService.search(1L, "포도 병해", 2, "bm25");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).retrievalSource()).isEqualTo("bm25");
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void askUsesChatClientWhenEnabledAndSearchContextExists() {
        RagService ragService = ragService(true);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("포도 탄저병은 병든 잎과 과실을 제거합니다.", 11L, 2)));
        given(chatClient.prompt().user(any(String.class)).call().content())
                .willReturn("병든 잎과 과실을 제거하고 방제 이력을 기록하세요.");

        RagAnswerResponse response = ragService.ask(1L, "포도 탄저병 대처", null);

        assertThat(response.answer()).contains("방제 이력");
        assertThat(response.sources()).hasSize(1);
    }

    @Test
    void askFallsBackToSearchEvidenceWhenLlmIsDisabled() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("포도 병해 의심 시 잎 뒷면과 과실 상태를 함께 확인합니다.", 12L, 1)));

        RagAnswerResponse response = ragService.ask(1L, "포도 병해 확인 방법", null);

        assertThat(response.answer()).contains("포도 병해 의심");
        assertThat(response.sources()).hasSize(1);
        verify(chatClient, never()).prompt();
    }

    @Test
    void askReturnsGuideWhenNoSearchResultExists() {
        RagService ragService = ragService(false);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        RagAnswerResponse response = ragService.ask(1L, "지원하지 않는 질문", null);

        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).isEmpty();
        verify(chatClient, never()).prompt();
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
        given(chatClientBuilder.defaultSystem(any(String.class))).willReturn(chatClientBuilder);
        given(chatClientBuilder.build()).willReturn(chatClient);
        return new RagService(
                vectorStore,
                bm25SearchClient,
                rerankerClient,
                chatClientBuilder,
                3,
                0.35,
                20,
                "test-embedding-model",
                llmEnabled
        );
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

    private RagSearchItem searchItem(String chunkId, Long documentId, int chunkIndex, String content, String source) {
        return new RagSearchItem(
                chunkId,
                documentId,
                chunkIndex,
                "grape-manual.pdf",
                content,
                1.0,
                null,
                1,
                null,
                source,
                null,
                null,
                null,
                null
        );
    }
}
