package com.nongsabu.backend.domain.document.dto;

import com.nongsabu.backend.domain.document.entity.DocumentAsset;

public record DocumentUploadResponse(
        Long id,
        String filename,
        String parsingStatus
) {

    public static DocumentUploadResponse from(DocumentAsset asset) {
        return new DocumentUploadResponse(asset.getId(), asset.getOriginalFilename(), asset.getParsingStatus().name());
    }
}

