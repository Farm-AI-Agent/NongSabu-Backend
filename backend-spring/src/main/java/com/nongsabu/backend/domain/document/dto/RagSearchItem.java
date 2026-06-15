package com.nongsabu.backend.domain.document.dto;

import com.nongsabu.backend.domain.document.entity.DocumentChunk;

public record RagSearchItem(
        Long chunkId,
        Long documentId,
        int chunkIndex,
        String content
) {

    public static RagSearchItem from(DocumentChunk chunk) {
        return new RagSearchItem(
                chunk.getId(),
                chunk.getDocumentAsset().getId(),
                chunk.getChunkIndex(),
                chunk.getContent()
        );
    }
}

