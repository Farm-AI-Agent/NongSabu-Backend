package com.nongsabu.backend.domain.agriculturedocument.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.agriculturedocument.entity.AgricultureDocumentChunk;

public record AgricultureDocumentChunkDto(
        Long id,
        Long documentId,
        Integer chunkIndex,
        String content,
        String embedding,
        String metadataJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AgricultureDocumentChunkDto from(AgricultureDocumentChunk chunk) {
        return new AgricultureDocumentChunkDto(
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getEmbedding(),
                chunk.getMetadataJson(),
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }
}

