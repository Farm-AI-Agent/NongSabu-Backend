package com.nongsabu.backend.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberProfileRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        String name
) {
}
