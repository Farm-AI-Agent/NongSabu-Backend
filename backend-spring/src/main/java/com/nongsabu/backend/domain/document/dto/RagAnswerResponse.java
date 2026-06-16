package com.nongsabu.backend.domain.document.dto;

import java.util.List;

public record RagAnswerResponse(
        String question,
        String answer,
        List<RagSearchItem> sources
) {
}
