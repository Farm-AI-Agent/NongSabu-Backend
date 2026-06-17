package com.nongsabu.backend.domain.debug.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.debug.dto.DebugIntegrationStatusResponse;
import com.nongsabu.backend.domain.debug.dto.DebugProbeResponse;
import com.nongsabu.backend.domain.debug.dto.DebugToolInvokeRequest;
import com.nongsabu.backend.domain.debug.dto.DebugToolInvokeResponse;
import com.nongsabu.backend.domain.debug.service.DebugIntegrationService;
import com.nongsabu.backend.domain.externalapilog.service.ExternalApiLogService;
import com.nongsabu.backend.security.CustomUserDetails;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class DebugIntegrationController {

    private final DebugIntegrationService debugIntegrationService;
    private final ExternalApiLogService externalApiLogService;

    @GetMapping("/integrations/status")
    public ApiResponse<DebugIntegrationStatusResponse> status() {
        return ApiResponse.ok("Integration status loaded.", debugIntegrationService.status());
    }

    @GetMapping("/integrations/probe/{target}")
    public ApiResponse<DebugProbeResponse> probe(@PathVariable String target) {
        return ApiResponse.ok("Probe completed.", debugIntegrationService.probe(target));
    }

    @GetMapping("/tools/samples")
    public ApiResponse<Map<String, Object>> samples() {
        return ApiResponse.ok("Debug tool sample payloads loaded.", debugIntegrationService.samplePayloads());
    }

    @PostMapping("/tools/invoke")
    public ApiResponse<DebugToolInvokeResponse> invoke(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody DebugToolInvokeRequest request
    ) {
        return ApiResponse.ok("Debug tool invocation completed.", debugIntegrationService.invoke(principal.id(), request));
    }

    @GetMapping("/tool-call-logs")
    public ApiResponse<List<Map<String, Object>>> toolCallLogs(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok("Tool call logs loaded.", externalApiLogService.getRecentToolCallLogs(limit));
    }
}
