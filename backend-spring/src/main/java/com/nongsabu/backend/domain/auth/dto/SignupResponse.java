package com.nongsabu.backend.domain.auth.dto;

public record SignupResponse(
        Long memberId,
        String email,
        String name
) {
}

