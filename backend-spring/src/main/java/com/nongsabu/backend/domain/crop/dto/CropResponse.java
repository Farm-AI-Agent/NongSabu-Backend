package com.nongsabu.backend.domain.crop.dto;

import com.nongsabu.backend.domain.crop.entity.Crop;

public record CropResponse(
        Long id,
        String name,
        String category,
        String description
) {

    public static CropResponse from(Crop crop) {
        return new CropResponse(crop.getId(), crop.getName(), crop.getCategory(), crop.getDescription());
    }
}

