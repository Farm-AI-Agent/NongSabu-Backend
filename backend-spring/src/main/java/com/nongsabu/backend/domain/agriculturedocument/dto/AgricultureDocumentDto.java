package com.nongsabu.backend.domain.agriculturedocument.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.agriculturedocument.entity.AgricultureDocument;
import com.nongsabu.backend.domain.agriculturedocument.entity.SourceType;

public record AgricultureDocumentDto(
        Long id,
        String title,
        String source,
        SourceType sourceType,
        String originalFilename,
        String storagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AgricultureDocumentDto from(AgricultureDocument document) {
        return new AgricultureDocumentDto(
                document.getId(),
                document.getTitle(),
                document.getSource(),
                document.getSourceType(),
                document.getOriginalFilename(),
                document.getStoragePath(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}

