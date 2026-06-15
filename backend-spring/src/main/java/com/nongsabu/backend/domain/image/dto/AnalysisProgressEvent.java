package com.nongsabu.backend.domain.image.dto;

import java.time.LocalDateTime;

public record AnalysisProgressEvent(
        Long imageId,
        String status,
        String message,
        LocalDateTime timestamp
) {
}

