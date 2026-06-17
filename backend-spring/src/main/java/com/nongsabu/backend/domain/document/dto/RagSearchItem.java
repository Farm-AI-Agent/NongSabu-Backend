package com.nongsabu.backend.domain.document.dto;

import java.util.Map;
import org.springframework.ai.document.Document;

public record RagSearchItem(
        String chunkId,
        Long documentId,
        int chunkIndex,
        String filename,
        String content,
        Double score,
        Integer denseRank,
        Integer bm25Rank,
        Double rrfScore,
        String retrievalSource,
        String logicalId,
        String sectionPath,
        Integer rerankRank,
        Double rerankScore
) {

    public static RagSearchItem from(Document document) {
        return from(document, null, null);
    }

    public static RagSearchItem from(Document document, Integer denseRank, String retrievalSource) {
        Map<String, Object> metadata = document.getMetadata();
        return new RagSearchItem(
                document.getId(),
                Long.valueOf(String.valueOf(metadata.get("documentId"))),
                Integer.parseInt(String.valueOf(metadata.get("chunkIndex"))),
                String.valueOf(metadata.get("filename")),
                document.getText(),
                document.getScore(),
                denseRank,
                null,
                null,
                retrievalSource,
                metadataString(metadata, "logicalId"),
                metadataString(metadata, "sectionPath"),
                null,
                null
        );
    }

    public RagSearchItem withDenseDiagnostics(Integer rank, String source) {
        return new RagSearchItem(
                chunkId,
                documentId,
                chunkIndex,
                filename,
                content,
                score,
                rank,
                bm25Rank,
                rrfScore,
                source,
                logicalId,
                sectionPath,
                rerankRank,
                rerankScore
        );
    }

    public RagSearchItem withBm25Diagnostics(Integer rank, String source) {
        return new RagSearchItem(
                chunkId,
                documentId,
                chunkIndex,
                filename,
                content,
                score,
                denseRank,
                rank,
                rrfScore,
                source,
                logicalId,
                sectionPath,
                rerankRank,
                rerankScore
        );
    }

    public RagSearchItem withHybridDiagnostics(
            Integer nextDenseRank,
            Integer nextBm25Rank,
            Double nextRrfScore,
            String nextRetrievalSource
    ) {
        return new RagSearchItem(
                chunkId,
                documentId,
                chunkIndex,
                filename,
                content,
                nextRrfScore,
                nextDenseRank,
                nextBm25Rank,
                nextRrfScore,
                nextRetrievalSource,
                logicalId,
                sectionPath,
                rerankRank,
                rerankScore
        );
    }

    public RagSearchItem withFallbackMetadataFrom(RagSearchItem other) {
        return new RagSearchItem(
                chunkId,
                documentId,
                chunkIndex,
                filename,
                content,
                score,
                denseRank,
                bm25Rank,
                rrfScore,
                retrievalSource,
                logicalId == null ? other.logicalId() : logicalId,
                sectionPath == null ? other.sectionPath() : sectionPath,
                rerankRank,
                rerankScore
        );
    }

    public RagSearchItem withRerankDiagnostics(Integer nextRerankRank, Double nextRerankScore) {
        return new RagSearchItem(
                chunkId,
                documentId,
                chunkIndex,
                filename,
                content,
                nextRerankScore,
                denseRank,
                bm25Rank,
                rrfScore,
                retrievalSource,
                logicalId,
                sectionPath,
                nextRerankRank,
                nextRerankScore
        );
    }

    private static String metadataString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }
}
