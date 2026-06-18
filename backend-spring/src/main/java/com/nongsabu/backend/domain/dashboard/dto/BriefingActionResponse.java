package com.nongsabu.backend.domain.dashboard.dto;

public record BriefingActionResponse(
        String priority,
        String title,
        String reason,
        String timeHint
) {
}
