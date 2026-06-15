package com.nongsabu.backend.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiAnalysisResponse(
        boolean success,
        String diagnosis,
        double confidence,
        String severity,
        String summary,
        @JsonProperty("recommended_action")
        String recommendedAction,
        @JsonProperty("model_version")
        String modelVersion
) {
}

