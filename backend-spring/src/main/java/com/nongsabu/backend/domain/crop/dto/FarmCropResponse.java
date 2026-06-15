package com.nongsabu.backend.domain.crop.dto;

import com.nongsabu.backend.domain.crop.entity.FarmCrop;

public record FarmCropResponse(
        Long id,
        Long cropId,
        String cropName,
        String status
) {

    public static FarmCropResponse from(FarmCrop farmCrop) {
        return new FarmCropResponse(
                farmCrop.getId(),
                farmCrop.getCrop().getId(),
                farmCrop.getCrop().getName(),
                farmCrop.getStatus()
        );
    }
}

