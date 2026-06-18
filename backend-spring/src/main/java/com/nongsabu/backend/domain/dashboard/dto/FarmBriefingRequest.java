package com.nongsabu.backend.domain.dashboard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FarmBriefingRequest(
        @NotNull @Valid WeatherSnapshotRequest weather,
        @Valid List<WeatherForecastRequest> forecast,
        Boolean forceRefresh
) {
    public boolean shouldForceRefresh() {
        return Boolean.TRUE.equals(forceRefresh);
    }
}
