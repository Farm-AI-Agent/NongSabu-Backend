package com.nongsabu.backend.domain.chat.dto;

import java.util.List;

public record ChatResponse(
        String question,
        String answer,
        String mode,
        boolean hasContext,
        List<String> sources
) {}
