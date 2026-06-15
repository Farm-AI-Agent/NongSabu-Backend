package com.nongsabu.backend.domain.document.dto;

import java.util.List;

public record RagSearchResponse(
        String query,
        List<RagSearchItem> items
) {
}

