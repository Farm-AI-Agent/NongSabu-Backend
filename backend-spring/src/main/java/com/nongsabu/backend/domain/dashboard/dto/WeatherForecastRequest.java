package com.nongsabu.backend.domain.dashboard.dto;

import java.time.LocalDate;

public record WeatherForecastRequest(
        LocalDate date,
        String sky,
        Double minTemperatureC,
        Double maxTemperatureC,
        Integer humidityPercent,
        Double precipitationMm,
        Integer precipitationProbability,
        Double windSpeedMs
) {
}
