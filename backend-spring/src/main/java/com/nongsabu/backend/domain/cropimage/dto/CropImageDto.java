package com.nongsabu.backend.domain.cropimage.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.cropimage.entity.CropImage;

public record CropImageDto(
        Long id,
        Long memberId,
        Long cropId,
        String originalFilename,
        String storedFilename,
        String storagePath,
        String contentType,
        long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CropImageDto from(CropImage cropImage) {
        return new CropImageDto(
                cropImage.getId(),
                cropImage.getMember().getId(),
                cropImage.getCrop() == null ? null : cropImage.getCrop().getId(),
                cropImage.getOriginalFilename(),
                cropImage.getStoredFilename(),
                cropImage.getStoragePath(),
                cropImage.getContentType(),
                cropImage.getFileSize(),
                cropImage.getCreatedAt(),
                cropImage.getUpdatedAt()
        );
    }
}

