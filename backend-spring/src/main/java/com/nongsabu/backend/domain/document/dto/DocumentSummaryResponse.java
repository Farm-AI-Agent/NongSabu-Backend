package com.nongsabu.backend.domain.document.dto;

import com.nongsabu.backend.domain.document.entity.DocumentAsset;

public record DocumentSummaryResponse(
        Long id,
        String filename,
        String contentType,
        String sourceType,
        String parsingStatus,
        boolean opensearchIndexed,
        String opensearchIndexError
) {

    public static DocumentSummaryResponse from(DocumentAsset asset) {
        return new DocumentSummaryResponse(
                asset.getId(),
                asset.getOriginalFilename(),
                asset.getContentType(),
                asset.getSourceType(),
                asset.getParsingStatus().name(),
                asset.isOpensearchIndexed(),
                asset.getOpensearchIndexError()
        );
    }
}

