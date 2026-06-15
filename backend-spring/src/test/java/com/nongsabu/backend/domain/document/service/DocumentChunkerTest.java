package com.nongsabu.backend.domain.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    private final DocumentChunker documentChunker = new DocumentChunker(1200, 200);

    @Test
    void chunksAtNearestSentenceBoundaryAndKeepsOverlap() {
        String firstSentence = "가".repeat(950) + ".";
        String text = firstSentence + "나".repeat(600);

        List<String> chunks = documentChunker.chunk(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst()).isEqualTo(firstSentence);
        assertThat(chunks.get(1)).startsWith(firstSentence.substring(firstSentence.length() - 200));
    }

    @Test
    void fallsBackToConfiguredSizeWhenSentenceBoundaryDoesNotExist() {
        String text = "가".repeat(1500);

        List<String> chunks = documentChunker.chunk(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst()).hasSize(1200);
        assertThat(chunks.get(1)).hasSize(500);
    }
}
