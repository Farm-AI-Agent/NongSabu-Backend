package com.nongsabu.backend.domain.document.dto;

import com.nongsabu.backend.domain.document.entity.DocumentAsset;

public record DocumentUploadResponse(
        Long id,
        String filename,
        String parsingStatus,
        int chunkCount,
        String embeddingModel
) {

    public static DocumentUploadResponse from(DocumentAsset asset, int chunkCount, String embeddingModel) {
        return new DocumentUploadResponse(
                asset.getId(),
                asset.getOriginalFilename(),
                asset.getParsingStatus().name(),
                chunkCount,
                embeddingModel
        );
    }
}

