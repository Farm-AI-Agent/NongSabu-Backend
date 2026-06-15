package com.nongsabu.backend.domain.usercrop.dto;

import jakarta.validation.constraints.NotNull;

public record UserCropRequest(
        @NotNull(message = "작물 ID를 입력해주세요.")
        Long cropId,

        String cultivationArea,

        String memo
) {
}
