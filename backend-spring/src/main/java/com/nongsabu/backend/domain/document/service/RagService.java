package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
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
            당신은 초보 농가를 돕는 농업 AI 비서입니다.
            검색된 문서 내용에 근거해 한국어로 명확하고 실용적으로 답하세요.
            문서에 답이 없으면 추측하지 말고 관련 정보가 없다고 말하세요.
            """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final double similarityThreshold;

    public RagService(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            @Value("${app.rag.top-k:4}") int topK,
            @Value("${app.rag.similarity-threshold:0.35}") double similarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.similarityThreshold = similarityThreshold;
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build())
                .build();
    }

    public RagSearchResponse search(Long userId, String query, int requestedTopK) {
        int safeTopK = Math.clamp(requestedTopK, 1, 20);
        List<RagSearchItem> items = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(safeTopK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(userFilter(userId))
                        .build())
                .stream()
                .map(RagSearchItem::from)
                .toList();
        return new RagSearchResponse(query, items);
    }

    public RagAnswerResponse ask(Long userId, String question) {
        String answer = chatClient.prompt()
                .user(question)
                .advisors(questionAnswerAdvisor)
                .advisors(advisor -> advisor.param(
                        QuestionAnswerAdvisor.FILTER_EXPRESSION,
                        userFilter(userId)
                ))
                .call()
                .content();
        return new RagAnswerResponse(question, answer);
    }

    public List<String> getContextSnippets(Long userId, String query, int requestedTopK) {
        return search(userId, query, requestedTopK).items().stream()
                .map(RagSearchItem::content)
                .toList();
    }

    private String userFilter(Long userId) {
        return "userId == '" + userId + "'";
    }
}
