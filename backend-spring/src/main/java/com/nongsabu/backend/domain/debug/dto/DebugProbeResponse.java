package com.nongsabu.backend.domain.debug.dto;

import java.util.Map;

public record DebugProbeResponse(
        String target,
        boolean configured,
        boolean attempted,
        boolean success,
        Integer statusCode,
        long elapsedMillis,
        String endpoint,
        Map<String, Object> requestSummary,
        String responsePreview,
        String errorMessage
) {
}
