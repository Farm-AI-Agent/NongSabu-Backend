package com.nongsabu.backend.domain.farm.dto;

import jakarta.validation.constraints.NotBlank;

public record FarmRequest(
        @NotBlank(message = "농장명은 필수입니다.")
        String name,
        String location,
        String cultivationArea,
        String notes
) {
}
