package com.nongsabu.backend.domain.document.dto;

import jakarta.validation.constraints.NotBlank;

public record RagAskRequest(
        @NotBlank(message = "질문은 필수입니다.")
        String question
) {
}
