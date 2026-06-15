package com.nongsabu.backend.domain.crop.dto;

import jakarta.validation.constraints.NotNull;

public record FarmCropRequest(
        @NotNull(message = "작물 ID는 필수입니다.")
        Long cropId,
        String status
) {
}

