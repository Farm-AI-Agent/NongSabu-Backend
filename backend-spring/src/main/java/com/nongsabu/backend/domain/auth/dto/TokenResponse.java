package com.nongsabu.backend.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String tokenType
) {
}

