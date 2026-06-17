package com.nongsabu.backend.domain.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.document.dto.OpenSearchReindexResponse;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
import com.nongsabu.backend.domain.document.repository.DocumentAssetRepository;
import com.nongsabu.backend.infra.search.opensearch.Bm25SearchClient;
import com.nongsabu.backend.infra.search.opensearch.OpenSearchIndexingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchReindexService {

    private static final String SELECT_VECTOR_CHUNKS = """
            SELECT id::text AS vector_row_id, content, metadata
            FROM vector_store
            WHERE metadata ->> 'memberId' = ?
            ORDER BY metadata ->> 'documentId',
                     CAST(NULLIF(metadata ->> 'chunkIndex', '') AS INTEGER) NULLS LAST
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Bm25SearchClient bm25SearchClient;
    private final DocumentAssetRepository documentAssetRepository;

    @Transactional
    public OpenSearchReindexResponse reindexCurrentUser(Long memberId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SELECT_VECTOR_CHUNKS, String.valueOf(memberId));

        int indexedCount = 0;
        List<OpenSearchReindexResponse.ReindexError> errors = new ArrayList<>();
        Map<Long, String> documentFailures = new LinkedHashMap<>();
        Map<Long, Boolean> touchedDocuments = new LinkedHashMap<>();

        for (int rowNumber = 0; rowNumber < rows.size(); rowNumber++) {
            VectorStoreChunk chunk;
            try {
                chunk = toChunk(memberId, rows.get(rowNumber), rowNumber);
            } catch (Exception exception) {
                String rowId = String.valueOf(rows.get(rowNumber).getOrDefault("vector_row_id", rowNumber));
                errors.add(new OpenSearchReindexResponse.ReindexError(rowId, exception.getMessage()));
                continue;
            }

            touchedDocuments.putIfAbsent(chunk.documentId(), true);
            try {
                bm25SearchClient.indexExistingChunk(
                        chunk.documentId(),
                        memberId,
                        chunk.chunkIndex(),
                        chunk.filename(),
                        chunk.content(),
                        chunk.logicalId(),
                        chunk.sectionPath()
                );
                indexedCount++;
            } catch (OpenSearchIndexingException exception) {
                String message = exception.getMessage();
                errors.add(new OpenSearchReindexResponse.ReindexError(chunk.chunkId(), message));
                documentFailures.putIfAbsent(chunk.documentId(), message);
                log.warn("OpenSearch reindex failed. memberId={}, chunkId={}", memberId, chunk.chunkId(), exception);
            }
        }

        updateDocumentIndexingStatus(touchedDocuments, documentFailures);
        return new OpenSearchReindexResponse(indexedCount, errors.size(), errors);
    }

    private VectorStoreChunk toChunk(Long memberId, Map<String, Object> row, int rowNumber) {
        String vectorRowId = String.valueOf(row.getOrDefault("vector_row_id", rowNumber));
        String content = row.get("content") == null ? "" : String.valueOf(row.get("content"));
        JsonNode metadata = metadataNode(row.get("metadata"));

        Long documentId = parseLong(metadataText(metadata, "documentId"), "documentId", vectorRowId);
        int chunkIndex = parseInt(metadataText(metadata, "chunkIndex"), "chunkIndex", vectorRowId);
        String filename = defaultIfBlank(metadataText(metadata, "filename"), "unknown.pdf");
        String logicalId = metadataText(metadata, "logicalId");
        String sectionPath = metadataText(metadata, "sectionPath");

        return new VectorStoreChunk(
                vectorRowId,
                documentId,
                memberId,
                chunkIndex,
                filename,
                content == null ? "" : content,
                logicalId,
                sectionPath
        );
    }

    private JsonNode metadataNode(Object metadata) {
        if (metadata == null) {
            return objectMapper.createObjectNode();
        }
        if (metadata instanceof JsonNode node) {
            return node;
        }
        try {
            return objectMapper.readTree(metadata.toString());
        } catch (Exception exception) {
            return objectMapper.valueToTree(metadata);
        }
    }

    private String metadataText(JsonNode metadata, String key) {
        JsonNode value = metadata.path(key);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private Long parseLong(String value, String fieldName, String vectorRowId) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("vector_store metadata missing " + fieldName + ". rowId=" + vectorRowId);
        }
        return Long.parseLong(value);
    }

    private int parseInt(String value, String fieldName, String vectorRowId) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("vector_store metadata missing " + fieldName + ". rowId=" + vectorRowId);
        }
        return Integer.parseInt(value);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void updateDocumentIndexingStatus(Map<Long, Boolean> touchedDocuments, Map<Long, String> failures) {
        if (touchedDocuments.isEmpty()) {
            return;
        }

        List<DocumentAsset> assets = documentAssetRepository.findAllById(touchedDocuments.keySet());
        for (DocumentAsset asset : assets) {
            String failure = failures.get(asset.getId());
            if (failure == null) {
                asset.markOpenSearchIndexed();
            } else {
                asset.markOpenSearchIndexFailed(failure);
            }
        }
    }

    private record VectorStoreChunk(
            String vectorRowId,
            Long documentId,
            Long memberId,
            int chunkIndex,
            String filename,
            String content,
            String logicalId,
            String sectionPath
    ) {
        private String chunkId() {
            return documentId + "-" + chunkIndex;
        }
    }
}
