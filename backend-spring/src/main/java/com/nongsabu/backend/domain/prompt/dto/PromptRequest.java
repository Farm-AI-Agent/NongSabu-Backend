package com.nongsabu.backend.domain.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PromptRequest(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("question") String question
) {}
