package com.nongsabu.backend.domain.document.dto;

import com.nongsabu.backend.domain.document.entity.DocumentAsset;

public record DocumentUploadResponse(
        Long id,
        String filename,
        String parsingStatus,
        boolean opensearchIndexed,
        String opensearchIndexError,
        int chunkCount,
        String embeddingModel
) {

    public static DocumentUploadResponse from(DocumentAsset asset, int chunkCount, String embeddingModel) {
        return new DocumentUploadResponse(
                asset.getId(),
                asset.getOriginalFilename(),
                asset.getParsingStatus().name(),
                asset.isOpensearchIndexed(),
                asset.getOpensearchIndexError(),
                chunkCount,
                embeddingModel
        );
    }
}

