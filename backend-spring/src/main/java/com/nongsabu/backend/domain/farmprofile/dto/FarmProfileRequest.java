package com.nongsabu.backend.domain.farmprofile.dto;

import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FarmProfileRequest(
        @NotBlank(message = "지역을 입력해주세요.")
        String region,

        @NotNull(message = "농사 경험 수준을 선택해주세요.")
        ExperienceLevel experienceLevel,

        @NotBlank(message = "농장 규모를 입력해주세요.")
        String farmSize,

        @NotBlank(message = "주 재배 작물을 입력해주세요.")
        String mainCrop
) {
}
