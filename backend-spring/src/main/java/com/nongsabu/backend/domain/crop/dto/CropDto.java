package com.nongsabu.backend.domain.crop.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.crop.entity.Crop;

public record CropDto(
        Long id,
        String name,
        String category,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CropDto from(Crop crop) {
        return new CropDto(
                crop.getId(),
                crop.getName(),
                crop.getCategory(),
                crop.getDescription(),
                crop.getCreatedAt(),
                crop.getUpdatedAt()
        );
    }
}

