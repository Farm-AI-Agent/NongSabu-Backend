package com.nongsabu.backend.domain.document.dto;

import java.util.Map;
import org.springframework.ai.document.Document;

public record RagSearchItem(
        String chunkId,
        Long documentId,
        int chunkIndex,
        String filename,
        String content,
        Double score
) {

    public static RagSearchItem from(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new RagSearchItem(
                document.getId(),
                Long.valueOf(String.valueOf(metadata.get("documentId"))),
                Integer.parseInt(String.valueOf(metadata.get("chunkIndex"))),
                String.valueOf(metadata.get("filename")),
                document.getText(),
                document.getScore()
        );
    }
}
