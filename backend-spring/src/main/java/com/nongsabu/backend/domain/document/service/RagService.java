package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.dto.RagDiagnosticResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
            당신은 초보·소규모 농가를 돕는 농업 AI 비서입니다.
            검색된 문서 내용을 근거로 한국어로 명확하고 실용적으로 답하세요.
            문서에 답이 없으면 추측하지 말고 관련 정보가 없다고 말하세요.
            """;
    private static final String DIAGNOSTIC_QUERY = "RAG 연결 상태 확인";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final int defaultTopK;
    private final double similarityThreshold;
    private final String embeddingModel;

    public RagService(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            @Value("${app.rag.top-k:4}") int topK,
            @Value("${app.rag.similarity-threshold:0.35}") double similarityThreshold,
            @Value("${app.embedding.model:text-embedding-3-small}") String embeddingModel
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.defaultTopK = topK;
        this.similarityThreshold = similarityThreshold;
        this.embeddingModel = embeddingModel;
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build())
                .build();
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
        String answer = chatClient.prompt()
                .user(question)
                .advisors(questionAnswerAdvisor)
                .advisors(advisor -> advisor.param(
                        QuestionAnswerAdvisor.FILTER_EXPRESSION,
                        memberFilter(memberId)
                ))
                .call()
                .content();
        return new RagAnswerResponse(question, answer);
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
                    "OpenAI embedding과 pgvector 검색 경로가 정상 응답했습니다.",
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

    private String memberFilter(Long memberId) {
        return "memberId == '" + memberId + "'";
    }
}
