package com.nongsabu.backend.domain.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingRequest;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingResponse;
import com.nongsabu.backend.domain.dashboard.dto.WeatherSnapshotRequest;
import com.nongsabu.backend.domain.dashboard.service.FarmBriefingService;
import com.nongsabu.backend.security.CustomUserDetails;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherFarmBriefingControllerTest {

    @Mock
    private FarmBriefingService farmBriefingService;

    @Test
    void weatherFarmBriefingCompatibilityPathDelegatesToBriefingService() {
        WeatherFarmBriefingController controller = new WeatherFarmBriefingController(farmBriefingService);
        CustomUserDetails principal = new CustomUserDetails(1L, "user@example.com", "password", "USER");
        FarmBriefingRequest request = request();
        FarmBriefingResponse response = new FarmBriefingResponse(
                LocalDate.of(2026, 6, 18),
                Instant.parse("2026-06-18T00:00:00Z"),
                false,
                true,
                "AI",
                "next-morning-or-weather-change",
                "Check drainage first today.",
                List.of(),
                List.of(),
                List.of("grape"),
                request.weather()
        );
        given(farmBriefingService.getTodayBriefing(1L, request)).willReturn(response);

        var result = controller.farmBriefing(principal, request);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isSameAs(response);
        verify(farmBriefingService).getTodayBriefing(1L, request);
    }

    private FarmBriefingRequest request() {
        return new FarmBriefingRequest(
                new WeatherSnapshotRequest(
                        "Naju",
                        OffsetDateTime.parse("2026-06-18T08:00:00+09:00"),
                        "cloudy",
                        24.0,
                        20.0,
                        29.0,
                        85,
                        3.0,
                        80,
                        4.0,
                        "southwest",
                        List.of("rain"),
                        null
                ),
                List.of(),
                false
        );
    }
}
