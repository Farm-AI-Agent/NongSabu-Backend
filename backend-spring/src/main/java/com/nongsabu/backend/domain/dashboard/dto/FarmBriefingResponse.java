package com.nongsabu.backend.domain.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FarmBriefingResponse(
        LocalDate briefingDate,
        Instant generatedAt,
        boolean cached,
        boolean aiGenerated,
        String source,
        String refreshAfter,
        String summary,
        List<BriefingActionResponse> actions,
        List<BriefingWarningResponse> warnings,
        List<String> cropNames,
        WeatherSnapshotRequest weather
) {
    public FarmBriefingResponse withCacheState(boolean cached) {
        return new FarmBriefingResponse(
                briefingDate,
                generatedAt,
                cached,
                aiGenerated,
                source,
                refreshAfter,
                summary,
                actions,
                warnings,
                cropNames,
                weather
        );
    }
}
