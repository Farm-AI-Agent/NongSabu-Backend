package com.nongsabu.backend.domain.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RagAskRequest(
        @NotBlank(message = "질문은 필수입니다.")
        String question,
        @Min(value = 1, message = "topK는 1 이상이어야 합니다.")
        @Max(value = 20, message = "topK는 20 이하여야 합니다.")
        Integer topK,
        String retrievalMode
) {
}
