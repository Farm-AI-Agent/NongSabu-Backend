package com.nongsabu.backend.domain.document.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagSearchRequest(
        @NotBlank(message = "검색어는 필수입니다.")
        String query,

        @Min(value = 1, message = "topK는 1 이상이어야 합니다.")
        int topK,

        String retrievalMode
) {
}

