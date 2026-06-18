package com.nongsabu.backend.domain.debug.dto;

import java.util.Map;

public record DebugToolInvokeRequest(
        String toolName,
        String query,
        Integer topK,
        String retrievalMode,
        String cropName,
        String prompt,
        String context,
        Map<String, Object> payload
) {
}
