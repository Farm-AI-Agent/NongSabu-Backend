package com.nongsabu.backend.domain.farm.dto;

import com.nongsabu.backend.domain.farm.entity.Farm;

public record FarmResponse(
        Long id,
        String name,
        String location,
        String cultivationArea,
        String notes
) {

    public static FarmResponse from(Farm farm) {
        return new FarmResponse(
                farm.getId(),
                farm.getName(),
                farm.getLocation(),
                farm.getCultivationArea(),
                farm.getNotes()
        );
    }
}
