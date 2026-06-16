package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {

    private static final int SENTENCE_BOUNDARY_LOOKBACK = 300;

    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentChunker(
            @Value("${app.document.chunk-size:1200}") int chunkSize,
            @Value("${app.document.chunk-overlap:200}") int chunkOverlap
    ) {
        if (chunkSize <= 0 || chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("문서 청킹 설정이 올바르지 않습니다.");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> chunk(String text) {
        String normalizedText = normalize(text);
        if (normalizedText.isBlank()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "PDF에서 추출된 텍스트가 없습니다.");
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalizedText.length()) {
            int targetEnd = Math.min(normalizedText.length(), start + chunkSize);
            int end = findChunkEnd(normalizedText, start, targetEnd);
            chunks.add(normalizedText.substring(start, end));
            if (end == normalizedText.length()) {
                break;
            }
            start = end - chunkOverlap;
        }
        return chunks;
    }

    private int findChunkEnd(String text, int start, int targetEnd) {
        if (targetEnd == text.length()) {
            return targetEnd;
        }

        int minimumEnd = Math.max(start + chunkOverlap + 1, targetEnd - SENTENCE_BOUNDARY_LOOKBACK);
        for (int index = targetEnd - 1; index >= minimumEnd; index--) {
            if (isSentenceBoundary(text.charAt(index))) {
                return index + 1;
            }
        }
        return targetEnd;
    }

    private boolean isSentenceBoundary(char character) {
        return character == '.'
                || character == '!'
                || character == '?'
                || character == '\u3002'
                || character == '\n';
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
