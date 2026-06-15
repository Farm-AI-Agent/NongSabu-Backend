package com.nongsabu.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String fullName,
        String phoneNumber,
        String region
) {
}

