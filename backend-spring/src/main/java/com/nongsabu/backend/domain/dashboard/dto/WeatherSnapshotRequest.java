package com.nongsabu.backend.domain.dashboard.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record WeatherSnapshotRequest(
        String location,
        OffsetDateTime observedAt,
        String sky,
        Double temperatureC,
        Double minTemperatureC,
        Double maxTemperatureC,
        Integer humidityPercent,
        Double precipitationMm,
        Integer precipitationProbability,
        Double windSpeedMs,
        String windDirection,
        List<String> alerts,
        Map<String, Object> raw
) {
}
