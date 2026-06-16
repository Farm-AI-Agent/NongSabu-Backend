package com.nongsabu.backend.domain.document.dto;

public record RagDiagnosticResponse(
        boolean ready,
        String status,
        String message,
        String embeddingModel,
        int topK,
        double similarityThreshold,
        int resultCount
) {
}
