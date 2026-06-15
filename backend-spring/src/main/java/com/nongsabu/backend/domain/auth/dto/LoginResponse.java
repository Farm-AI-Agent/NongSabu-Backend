package com.nongsabu.backend.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

