package com.nongsabu.backend.domain.prompt.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PromptProgressEvent(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") LocalDateTime timestamp
) {}
