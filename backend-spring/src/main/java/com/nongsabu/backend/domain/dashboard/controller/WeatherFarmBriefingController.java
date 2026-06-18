package com.nongsabu.backend.domain.dashboard.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingRequest;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingResponse;
import com.nongsabu.backend.domain.dashboard.service.FarmBriefingService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WeatherFarmBriefingController {

    private final FarmBriefingService farmBriefingService;

    @PostMapping({"/weather-farm-briefing", "/farm-briefing"})
    public ApiResponse<FarmBriefingResponse> farmBriefing(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody FarmBriefingRequest request
    ) {
        return ApiResponse.ok("Farm briefing loaded.", farmBriefingService.getTodayBriefing(principal.id(), request));
    }
}
