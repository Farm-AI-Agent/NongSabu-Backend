package com.nongsabu.backend.domain.debug.dto;

public record DebugToolInvokeResponse(
        String toolName,
        boolean success,
        Object result,
        String errorMessage,
        long elapsedMillis
) {
}
