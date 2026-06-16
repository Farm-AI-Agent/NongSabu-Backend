package com.nongsabu.backend.domain.farmprofile.dto;

import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import java.time.LocalDateTime;

public record FarmProfileDto(
        Long id,
        Long memberId,
        String region,
        ExperienceLevel experienceLevel,
        String farmSize,
        Long mainCropId,
        String mainCropName,
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
                farmProfile.getMainCrop() == null ? null : farmProfile.getMainCrop().getId(),
                farmProfile.getMainCrop() == null ? null : farmProfile.getMainCrop().getName(),
                farmProfile.getCreatedAt(),
                farmProfile.getUpdatedAt()
        );
    }
}
