package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.dto.RagDiagnosticResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.domain.externalapilog.service.ExternalApiLogService;
import com.nongsabu.backend.infra.ai.llm.LlmClient;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private static final String DIAGNOSTIC_QUERY = "RAG 연결 상태 확인";

    private final VectorStore vectorStore;
    private final LlmClient llmClient;
    private final ExternalApiLogService externalApiLogService;
    private final int defaultTopK;
    private final double similarityThreshold;
    private final String embeddingModel;
    private final boolean llmEnabled;

    public RagService(
            VectorStore vectorStore,
            LlmClient llmClient,
            ExternalApiLogService externalApiLogService,
            @Value("${app.rag.top-k:4}") int topK,
            @Value("${app.rag.similarity-threshold:0.35}") double similarityThreshold,
            @Value("${app.embedding.model:text-embedding-3-small}") String embeddingModel,
            @Value("${app.llm.enabled:false}") boolean llmEnabled
    ) {
        this.vectorStore = vectorStore;
        this.llmClient = llmClient;
        this.externalApiLogService = externalApiLogService;
        this.defaultTopK = topK;
        this.similarityThreshold = similarityThreshold;
        this.embeddingModel = embeddingModel;
        this.llmEnabled = llmEnabled;
    }

    public RagSearchResponse search(Long memberId, String query, int requestedTopK) {
        int safeTopK = Math.max(1, Math.min(requestedTopK, 20));
        List<RagSearchItem> items = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(safeTopK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(memberFilter(memberId))
                        .build())
                .stream()
                .map(RagSearchItem::from)
                .toList();
        return new RagSearchResponse(query, items);
    }

    public RagAnswerResponse ask(Long memberId, String question) {
        RagSearchResponse searchResponse = search(memberId, question, defaultTopK);
        String context = buildContext(searchResponse.items());

        if (llmEnabled && StringUtils.isNotBlank(context)) {
            String prompt = buildPrompt(question);
            try {
                String generated = llmClient.generate(prompt, context);
                boolean success = StringUtils.isNotBlank(generated);
                externalApiLogService.logLlmGeneration(
                        memberId,
                        null,
                        "rag-ask",
                        prompt,
                        context,
                        generated,
                        success,
                        success ? null : "LLM 응답이 비어 있습니다."
                );
                if (success) {
                    return new RagAnswerResponse(question, generated);
                }
            } catch (RuntimeException exception) {
                externalApiLogService.logLlmGeneration(
                        memberId,
                        null,
                        "rag-ask",
                        prompt,
                        context,
                        null,
                        false,
                        exception.getMessage()
                );
            }
        }

        return new RagAnswerResponse(question, buildFallbackAnswer(searchResponse.items()));
    }

    public List<String> getContextSnippets(Long memberId, String query, int requestedTopK) {
        return search(memberId, query, requestedTopK).items().stream()
                .map(RagSearchItem::content)
                .toList();
    }

    public RagDiagnosticResponse diagnose(Long memberId) {
        try {
            List<RagSearchItem> items = search(memberId, DIAGNOSTIC_QUERY, 1).items();
            return new RagDiagnosticResponse(
                    true,
                    "READY",
                    "embedding과 pgvector 검색 경로가 정상 응답했습니다.",
                    embeddingModel,
                    defaultTopK,
                    similarityThreshold,
                    items.size()
            );
        } catch (Exception exception) {
            return new RagDiagnosticResponse(
                    false,
                    "FAILED",
                    "RAG 검색 경로 확인에 실패했습니다: " + exception.getMessage(),
                    embeddingModel,
                    defaultTopK,
                    similarityThreshold,
                    0
            );
        }
    }

    private String buildPrompt(String question) {
        return """
                당신은 초보·소규모 농가를 돕는 농업 AI 비서입니다.
                검색된 문서 내용을 근거로 한국어로 명확하고 실용적으로 답하세요.
                문서에 답이 없으면 추측하지 말고 관련 정보가 없다고 말하세요.

                질문:
                %s
                """.formatted(question);
    }

    private String buildContext(List<RagSearchItem> items) {
        return items.stream()
                .map(item -> """
                        [문서: %s / chunk: %d]
                        %s
                        """.formatted(item.filename(), item.chunkIndex(), item.content()))
                .reduce((left, right) -> left + "\n---\n" + right)
                .orElse("");
    }

    private String buildFallbackAnswer(List<RagSearchItem> items) {
        if (items.isEmpty()) {
            return "업로드된 문서에서 관련 근거를 찾지 못했습니다. 농업 매뉴얼 문서를 먼저 업로드한 뒤 다시 질문해주세요.";
        }

        String references = buildContext(items);
        // 로컬 MVP에서는 OpenAI 키가 없어도 RAG 검색 결과를 확인할 수 있도록 근거 중심 요약을 제공한다.
        return """
                현재 LLM 생성이 비활성화되어 있어 검색된 문서 근거를 우선 제공합니다.

                %s
                """.formatted(references);
    }

    private String memberFilter(Long memberId) {
        return "memberId == '" + memberId + "'";
    }
}
