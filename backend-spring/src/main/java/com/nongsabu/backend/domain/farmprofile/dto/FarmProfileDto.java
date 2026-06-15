package com.nongsabu.backend.domain.farmprofile.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;

public record FarmProfileDto(
        Long id,
        Long memberId,
        String region,
        ExperienceLevel experienceLevel,
        String farmSize,
        String mainCrop,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FarmProfileDto from(FarmProfile farmProfile) {
        return new FarmProfileDto(
                farmProfile.getId(),
                farmProfile.getMember().getId(),
                farmProfile.getRegion(),
                farmProfile.getExperienceLevel(),
                farmProfile.getFarmSize(),
                farmProfile.getMainCrop(),
                farmProfile.getCreatedAt(),
                farmProfile.getUpdatedAt()
        );
    }
}

