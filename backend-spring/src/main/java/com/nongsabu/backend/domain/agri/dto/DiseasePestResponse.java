package com.nongsabu.backend.domain.agri.dto;

import java.util.List;

public record DiseasePestResponse(
        String query,
        int totalCount,
        List<Item> items
) {
    public record Item(
            String korName,
            String divName,
            String cropName,
            String oprName,
            String thumbImg,
            String detailUrl
    ) {}
}
