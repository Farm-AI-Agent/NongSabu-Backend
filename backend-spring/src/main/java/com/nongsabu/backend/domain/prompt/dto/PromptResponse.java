package com.nongsabu.backend.domain.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PromptResponse(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("answer") String answer,
        @JsonProperty("references") List<String> references,
        @JsonProperty("confidence") String confidence
) {}
