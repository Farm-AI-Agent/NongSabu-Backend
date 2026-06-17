package com.nongsabu.backend.domain.debug.dto;

import java.util.List;

public record DebugIntegrationStatusResponse(
        List<ApiKeyStatus> apiKeys,
        List<EndpointStatus> endpoints,
        List<String> availableTools
) {

    public record ApiKeyStatus(
            String name,
            boolean configured,
            String propertyName,
            String maskedValue,
            String note
    ) {
    }

    public record EndpointStatus(
            String name,
            String baseUrl,
            String note
    ) {
    }
}
