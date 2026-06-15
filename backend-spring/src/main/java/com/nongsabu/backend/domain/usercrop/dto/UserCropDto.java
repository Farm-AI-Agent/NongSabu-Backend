package com.nongsabu.backend.domain.usercrop.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.usercrop.entity.UserCrop;

public record UserCropDto(
        Long id,
        Long memberId,
        Long cropId,
        String cropName,
        String cultivationArea,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserCropDto from(UserCrop userCrop) {
        return new UserCropDto(
                userCrop.getId(),
                userCrop.getMember().getId(),
                userCrop.getCrop().getId(),
                userCrop.getCrop().getName(),
                userCrop.getCultivationArea(),
                userCrop.getMemo(),
                userCrop.getCreatedAt(),
                userCrop.getUpdatedAt()
        );
    }
}

