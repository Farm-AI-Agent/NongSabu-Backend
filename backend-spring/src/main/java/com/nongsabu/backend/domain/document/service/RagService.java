package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.dto.RagDiagnosticResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.infra.reranker.RerankerClient;
import com.nongsabu.backend.infra.search.opensearch.Bm25SearchClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RagService {

    private static final String DIAGNOSTIC_QUERY = "RAG 연결 상태 확인";
    private static final String SYSTEM_PROMPT = """
            당신은 초보 농가를 돕는 농업 AI 비서입니다.
            검색된 문서 내용에 근거해 한국어로 명확하고 실용적으로 답하세요.
            문서에 답이 없으면 추측하지 말고 관련 정보가 없다고 말하세요.
            """;

    private final VectorStore vectorStore;
    private final Bm25SearchClient bm25SearchClient;
    private final RerankerClient rerankerClient;
    private final ChatClient chatClient;
    private final int defaultTopK;
    private final double similarityThreshold;
    private final int hybridCandidateSize;
    private final String embeddingModel;
    private final boolean llmEnabled;

    public RagService(
            VectorStore vectorStore,
            Bm25SearchClient bm25SearchClient,
            RerankerClient rerankerClient,
            ChatClient.Builder chatClientBuilder,
            @Value("${app.rag.top-k:4}") int topK,
            @Value("${app.rag.similarity-threshold:0.35}") double similarityThreshold,
            @Value("${app.rag.hybrid-candidate-size:20}") int hybridCandidateSize,
            @Value("${app.embedding.model:text-embedding-3-small}") String embeddingModel,
            @Value("${app.llm.enabled:false}") boolean llmEnabled
    ) {
        this.vectorStore = vectorStore;
        this.bm25SearchClient = bm25SearchClient;
        this.rerankerClient = rerankerClient;
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.defaultTopK = topK;
        this.similarityThreshold = similarityThreshold;
        this.hybridCandidateSize = hybridCandidateSize;
        this.embeddingModel = embeddingModel;
        this.llmEnabled = llmEnabled;
    }

    public RagSearchResponse search(Long memberId, String query, int requestedTopK) {
        return search(memberId, query, requestedTopK, null);
    }

    public RagSearchResponse search(Long memberId, String query, int requestedTopK, String requestedMode) {
        int safeTopK = Math.max(1, Math.min(requestedTopK, 20));
        RetrievalMode mode = RetrievalMode.from(requestedMode);
        List<RagSearchItem> items = switch (mode) {
            case VECTOR -> vectorSearch(memberId, query, safeTopK);
            case BM25 -> bm25SearchClient.search(memberId, query, safeTopK);
            case HYBRID -> hybridSearch(memberId, query, safeTopK);
            case HYBRID_RERANK -> hybridRerankSearch(memberId, query, safeTopK);
        };
        return new RagSearchResponse(query, items);
    }

    private List<RagSearchItem> vectorSearch(Long memberId, String query, int topK) {
        List<org.springframework.ai.document.Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(memberFilter(memberId))
                        .build());
        List<RagSearchItem> items = new ArrayList<>();
        for (int index = 0; index < documents.size(); index++) {
            items.add(RagSearchItem.from(documents.get(index), index + 1, "dense"));
        }
        return items;
    }

    private List<RagSearchItem> hybridSearch(Long memberId, String query, int topK) {
        int candidateSize = Math.max(topK, Math.max(1, hybridCandidateSize));
        List<RagSearchItem> vectorItems = vectorSearch(memberId, query, candidateSize);
        List<RagSearchItem> bm25Items = bm25SearchClient.search(memberId, query, candidateSize);
        return fuseByRrf(vectorItems, bm25Items, topK);
    }

    private List<RagSearchItem> hybridRerankSearch(Long memberId, String query, int topK) {
        int candidateSize = Math.max(topK, Math.max(1, hybridCandidateSize));
        List<RagSearchItem> vectorItems = vectorSearch(memberId, query, candidateSize);
        List<RagSearchItem> bm25Items = bm25SearchClient.search(memberId, query, candidateSize);
        List<RagSearchItem> rrfCandidates = fuseByRrf(vectorItems, bm25Items, candidateSize);
        return rerankerClient.rerank(query, rrfCandidates, topK);
    }

    public RagAnswerResponse ask(Long memberId, String question, Integer requestedTopK) {
        return ask(memberId, question, requestedTopK, null);
    }

    public RagAnswerResponse ask(Long memberId, String question, Integer requestedTopK, String requestedMode) {
        int topK = requestedTopK == null ? defaultTopK : requestedTopK;
        List<RagSearchItem> sources = search(memberId, question, topK, requestedMode).items();
        if (sources.isEmpty()) {
            return new RagAnswerResponse(
                    question,
                    "업로드된 문서에서 질문과 관련된 정보를 찾을 수 없습니다.",
                    sources
            );
        }

        if (!llmEnabled) {
            return new RagAnswerResponse(question, buildFallbackAnswer(sources), sources);
        }

        String context = sources.stream()
                .map(item -> "[문서: %s, 청크: %d, 점수: %s]\n%s".formatted(
                        item.filename(),
                        item.chunkIndex(),
                        item.score() == null ? "-" : item.score(),
                        item.content()
                ))
                .reduce((left, right) -> left + "\n\n---\n\n" + right)
                .orElse("");

        try {
            String answer = chatClient.prompt()
                    .user("""
                            다음 검색 문맥만 근거로 질문에 답하세요.
                            문맥에 근거가 없으면 관련 정보를 찾을 수 없다고 답하세요.

                            [검색 문맥]
                            %s

                            [질문]
                            %s
                            """.formatted(context, question))
                    .call()
                    .content();
            return new RagAnswerResponse(question, answer, sources);
        } catch (RuntimeException exception) {
            log.warn("RAG answer generation failed. Falling back to source summary.", exception);
            return new RagAnswerResponse(question, buildFallbackAnswer(sources), sources);
        }
    }

    public List<String> getContextSnippets(Long memberId, String query, int requestedTopK) {
        return search(memberId, query, requestedTopK).items().stream()
                .map(RagSearchItem::content)
                .toList();
    }

    public RagDiagnosticResponse diagnose(Long memberId) {
        try {
            int resultCount = search(memberId, DIAGNOSTIC_QUERY, 1).items().size();
            return new RagDiagnosticResponse(
                    true,
                    "READY",
                    "embedding과 검색 경로가 정상 응답했습니다.",
                    embeddingModel,
                    defaultTopK,
                    similarityThreshold,
                    resultCount
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

    private List<RagSearchItem> fuseByRrf(
            List<RagSearchItem> vectorItems,
            List<RagSearchItem> bm25Items,
            int topK
    ) {
        Map<String, ScoredSearchItem> merged = new LinkedHashMap<>();
        addRrfScores(merged, vectorItems, true);
        addRrfScores(merged, bm25Items, false);
        return merged.values().stream()
                .sorted(Comparator.comparing(ScoredSearchItem::score).reversed())
                .limit(topK)
                .map(ScoredSearchItem::toSearchItem)
                .toList();
    }

    private void addRrfScores(Map<String, ScoredSearchItem> merged, List<RagSearchItem> items, boolean dense) {
        int rrfK = 60;
        for (int index = 0; index < items.size(); index++) {
            RagSearchItem item = items.get(index);
            int rank = index + 1;
            double rrfScore = 1.0 / (rrfK + index + 1);
            merged.compute(itemKey(item), (key, current) -> {
                if (current == null) {
                    return dense
                            ? ScoredSearchItem.fromDense(item, rank, rrfScore)
                            : ScoredSearchItem.fromBm25(item, rank, rrfScore);
                }
                return dense ? current.addDense(item, rank, rrfScore) : current.addBm25(item, rank, rrfScore);
            });
        }
    }

    private String itemKey(RagSearchItem item) {
        return item.documentId() + ":" + item.chunkIndex();
    }

    private String memberFilter(Long memberId) {
        return "memberId == '" + memberId + "'";
    }

    private String buildFallbackAnswer(List<RagSearchItem> items) {
        if (items.isEmpty()) {
            return "업로드된 문서에서 관련 근거를 찾지 못했습니다. 농업 문서를 업로드한 뒤 다시 질문해주세요.";
        }

        String references = items.stream()
                .map(item -> """
                        [문서: %s / 청크: %d]
                        %s
                        """.formatted(item.filename(), item.chunkIndex(), item.content()))
                .reduce((left, right) -> left + "\n---\n" + right)
                .orElse("");

        // OpenAI 키가 없거나 LLM 호출이 실패해도 검색 근거는 바로 확인할 수 있게 유지한다.
        return """
                현재 LLM 생성이 비활성화되어 있어 검색된 문서 근거를 우선 제공합니다.

                %s
                """.formatted(references);
    }

    private record ScoredSearchItem(RagSearchItem item, double score, Integer denseRank, Integer bm25Rank) {

        private static ScoredSearchItem fromDense(RagSearchItem item, int rank, double score) {
            return new ScoredSearchItem(item, score, rank, null);
        }

        private static ScoredSearchItem fromBm25(RagSearchItem item, int rank, double score) {
            return new ScoredSearchItem(item, score, null, rank);
        }

        private ScoredSearchItem addDense(RagSearchItem nextItem, int rank, double additionalScore) {
            return new ScoredSearchItem(
                    item.withFallbackMetadataFrom(nextItem),
                    score + additionalScore,
                    firstNonNull(denseRank, rank),
                    bm25Rank
            );
        }

        private ScoredSearchItem addBm25(RagSearchItem nextItem, int rank, double additionalScore) {
            return new ScoredSearchItem(
                    item.withFallbackMetadataFrom(nextItem),
                    score + additionalScore,
                    denseRank,
                    firstNonNull(bm25Rank, rank)
            );
        }

        private RagSearchItem toSearchItem() {
            return item.withHybridDiagnostics(denseRank, bm25Rank, score, retrievalSource());
        }

        private String retrievalSource() {
            if (denseRank != null && bm25Rank != null) {
                return "hybrid_both";
            }
            if (denseRank != null) {
                return "hybrid_dense_only";
            }
            return "hybrid_bm25_only";
        }

        private Integer firstNonNull(Integer current, Integer next) {
            return current == null ? next : current;
        }
    }
}
