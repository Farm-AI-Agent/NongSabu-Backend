package com.nongsabu.backend.infra.search.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
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
public class Bm25SearchClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final String indexName;
    private final Duration readTimeout;

    public Bm25SearchClient(
            WebClient webClient,
            @Value("${app.opensearch.base-url}") String baseUrl,
            @Value("${app.opensearch.index-name}") String indexName,
            @Value("${app.opensearch.read-timeout:10s}") Duration readTimeout
    ) {
        this.webClient = webClient;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.indexName = indexName;
        this.readTimeout = readTimeout;
    }

    public void indexChunks(DocumentAsset asset, Long memberId, List<String> chunks) {
        indexChunks(asset, memberId, chunks, "MEMBER");
    }

    public void indexChunks(DocumentAsset asset, Long memberId, List<String> chunks, String scope) {
        verifyIndexExists();
        for (int index = 0; index < chunks.size(); index++) {
            indexChunk(
                    asset.getId(),
                    memberId,
                    scope,
                    index,
                    asset.getOriginalFilename(),
                    chunks.get(index),
                    null,
                    asset.getOriginalFilename()
            );
        }
    }

    public void indexExistingChunk(
            Long documentId,
            Long memberId,
            int chunkIndex,
            String filename,
            String content,
            String logicalId,
            String sectionPath
    ) {
        verifyIndexExists();
        indexChunk(documentId, memberId, "MEMBER", chunkIndex, filename, content, logicalId, sectionPath);
    }

    public List<RagSearchItem> search(Long memberId, String query, int topK) {
        Map<String, Object> requestBody = Map.of(
                "size", Math.max(1, topK),
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(Map.of(
                                        "bool", Map.of(
                                                "should", List.of(
                                                        Map.of("term", Map.of("member_id", String.valueOf(memberId))),
                                                        Map.of("term", Map.of("scope", "GLOBAL"))
                                                ),
                                                "minimum_should_match", 1
                                        )
                                )),
                                "should", List.of(Map.of(
                                        "multi_match", Map.of(
                                                "query", query,
                                                "fields", List.of(
                                                        "title^5",
                                                        "aliases^4",
                                                        "symptom_keywords^3",
                                                        "section_path^2",
                                                        "body",
                                                        "search_text"
                                                ),
                                                "type", "best_fields"
                                        )
                                )),
                                "minimum_should_match", 1
                        )
                )
        );

        try {
            JsonNode response = webClient.post()
                    .uri(indexUrl("_search"))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(readTimeout);
            return parseHits(response);
        } catch (Exception exception) {
            log.warn("OpenSearch BM25 search failed. query={}", query, exception);
            return List.of();
        }
    }

    private void indexChunk(
            Long documentId,
            Long memberId,
            String scope,
            int chunkIndex,
            String filename,
            String content,
            String logicalId,
            String sectionPath
    ) {
        String safeFilename = filename == null || filename.isBlank() ? "unknown.pdf" : filename;
        String safeSectionPath = sectionPath == null || sectionPath.isBlank() ? safeFilename : sectionPath;
        String chunkId = chunkId(documentId, chunkIndex);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("chunk_id", chunkId);
        document.put("member_id", String.valueOf(memberId));
        document.put("scope", scope == null || scope.isBlank() ? "MEMBER" : scope);
        document.put("document_id", String.valueOf(documentId));
        document.put("chunk_index", chunkIndex);
        document.put("filename", safeFilename);
        document.put("title", safeFilename);
        document.put("aliases", List.of());
        document.put("symptom_keywords", List.of());
        document.put("logical_id", logicalId);
        document.put("section_path", safeSectionPath);
        document.put("body", content);
        document.put("search_text", safeFilename + "\n" + safeSectionPath + "\n" + content);

        try {
            webClient.put()
                    .uri(indexUrl("_doc/" + chunkId + "?refresh=wait_for"))
                    .bodyValue(document)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(readTimeout);
        } catch (Exception exception) {
            throw new OpenSearchIndexingException("OpenSearch chunk indexing failed. chunkId=" + chunkId, exception);
        }
    }

    private void verifyIndexExists() {
        try {
            webClient.head()
                    .uri(baseUrl + "/" + indexName)
                    .retrieve()
                    .toBodilessEntity()
                    .block(readTimeout);
        } catch (Exception exception) {
            createIndex(exception);
        }
    }

    private void createIndex(Exception cause) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("chunk_id", Map.of("type", "keyword"));
        properties.put("member_id", Map.of("type", "keyword"));
        properties.put("scope", Map.of("type", "keyword"));
        properties.put("document_id", Map.of("type", "keyword"));
        properties.put("chunk_index", Map.of("type", "integer"));
        properties.put("filename", Map.of("type", "keyword"));
        properties.put("logical_id", Map.of("type", "keyword"));
        properties.put("title", Map.of("type", "text"));
        properties.put("aliases", Map.of("type", "text"));
        properties.put("symptom_keywords", Map.of("type", "text"));
        properties.put("section_path", Map.of("type", "text"));
        properties.put("body", Map.of("type", "text"));
        properties.put("search_text", Map.of("type", "text"));

        Map<String, Object> requestBody = Map.of(
                "settings", Map.of(
                        "index", Map.of(
                                "similarity", Map.of(
                                        "default", Map.of("type", "BM25")
                                )
                        )
                ),
                "mappings", Map.of("properties", properties)
        );

        try {
            webClient.put()
                    .uri(baseUrl + "/" + indexName)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(readTimeout);
            log.info("OpenSearch index created automatically. indexName={}", indexName);
        } catch (Exception exception) {
            throw new OpenSearchIndexingException(
                    "OpenSearch index is not ready and automatic creation failed. indexName=" + indexName,
                    cause
            );
        }
    }

    private List<RagSearchItem> parseHits(JsonNode response) {
        JsonNode hits = response == null ? null : response.path("hits").path("hits");
        if (hits == null || !hits.isArray()) {
            return List.of();
        }

        List<RagSearchItem> items = new ArrayList<>();
        for (int index = 0; index < hits.size(); index++) {
            JsonNode hit = hits.get(index);
            JsonNode source = hit.path("_source");
            Long documentId = parseLong(source.path("document_id").asText(null));
            int chunkIndex = source.path("chunk_index").asInt(0);
            items.add(new RagSearchItem(
                    source.path("chunk_id").asText(hit.path("_id").asText()),
                    documentId,
                    chunkIndex,
                    source.path("filename").asText("unknown.pdf"),
                    source.path("body").asText(""),
                    hit.path("_score").isNumber() ? hit.path("_score").asDouble() : null,
                    null,
                    index + 1,
                    null,
                    "bm25",
                    textOrNull(source.path("logical_id")),
                    textOrNull(source.path("section_path")),
                    null,
                    null
            ));
        }
        return items;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private String indexUrl(String path) {
        return baseUrl + "/" + indexName + "/" + path;
    }

    private String chunkId(Long documentId, int chunkIndex) {
        return documentId + "-" + chunkIndex;
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:9200";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
