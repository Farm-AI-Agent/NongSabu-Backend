package com.nongsabu.backend.infra.reranker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class RerankerClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final Duration readTimeout;
    private final boolean enabled;

    public RerankerClient(
            WebClient webClient,
            @Value("${app.reranker.base-url}") String baseUrl,
            @Value("${app.reranker.read-timeout:180s}") Duration readTimeout,
            @Value("${app.reranker.enabled:true}") boolean enabled
    ) {
        this.webClient = webClient;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.readTimeout = readTimeout;
        this.enabled = enabled;
    }

    public List<RagSearchItem> rerank(String query, List<RagSearchItem> candidates, int topN) {
        int safeTopN = Math.max(1, Math.min(topN, candidates.size()));
        if (!enabled || candidates.isEmpty()) {
            return candidates.stream().limit(safeTopN).toList();
        }

        Map<String, RagSearchItem> byChunkId = new LinkedHashMap<>();
        for (RagSearchItem candidate : candidates) {
            byChunkId.put(candidate.chunkId(), candidate);
        }

        RerankRequest request = new RerankRequest(
                query,
                safeTopN,
                candidates.stream().map(this::toCandidate).toList()
        );

        try {
            RerankResponse response = webClient.post()
                    .uri(baseUrl + "/api/v1/rerank")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RerankResponse.class)
                    .block(readTimeout);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return candidates.stream().limit(safeTopN).toList();
            }
            if (response.fallbackUsed()) {
                log.warn("Reranker server used fallback scoring. model={}, backend={}",
                        response.model(), response.backend());
            }

            List<RagSearchItem> reranked = new ArrayList<>();
            for (RerankResult result : response.results()) {
                RagSearchItem item = byChunkId.get(result.chunkId());
                if (item != null) {
                    reranked.add(item.withRerankDiagnostics(result.rerankRank(), result.rerankScore()));
                }
            }
            return reranked.isEmpty() ? candidates.stream().limit(safeTopN).toList() : reranked;
        } catch (Exception exception) {
            log.warn(
                    "Reranker call failed. Falling back to pre-rerank order. type={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            return candidates.stream().limit(safeTopN).toList();
        }
    }

    private RerankCandidate toCandidate(RagSearchItem item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("document_id", item.documentId());
        metadata.put("chunk_index", item.chunkIndex());
        metadata.put("filename", item.filename());
        metadata.put("retrieval_source", item.retrievalSource());
        metadata.put("dense_rank", item.denseRank());
        metadata.put("bm25_rank", item.bm25Rank());
        metadata.put("rrf_score", item.rrfScore());
        metadata.put("logical_id", item.logicalId());
        metadata.put("section_path", item.sectionPath());
        return new RerankCandidate(item.chunkId(), item.content(), metadata);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8010";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record RerankRequest(
            String query,
            @JsonProperty("top_n")
            int topN,
            List<RerankCandidate> candidates
    ) {
    }

    private record RerankCandidate(
            @JsonProperty("chunk_id")
            String chunkId,
            String text,
            Map<String, Object> metadata
    ) {
    }

    private record RerankResponse(
            String model,
            String backend,
            @JsonProperty("fallback_used")
            boolean fallbackUsed,
            List<RerankResult> results
    ) {
    }

    private record RerankResult(
            @JsonProperty("chunk_id")
            String chunkId,
            @JsonProperty("rerank_rank")
            Integer rerankRank,
            @JsonProperty("rerank_score")
            Double rerankScore
    ) {
    }
}
