package com.nongsabu.backend.domain.policy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PolicySyncRequest(
        Boolean includeGov24,
        Boolean includeYoungFarmer,
        String keyword,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer size,
        @Min(1) @Max(500) Integer maxPages
) {

    public boolean shouldIncludeGov24() {
        return includeGov24 == null || includeGov24;
    }

    public boolean shouldIncludeYoungFarmer() {
        return includeYoungFarmer == null || includeYoungFarmer;
    }

    public int safePage() {
        return page == null ? 1 : page;
    }

    public int safeSize() {
        return size == null ? 20 : size;
    }

    public int safeMaxPages() {
        return maxPages == null ? 10 : maxPages;
    }
}
