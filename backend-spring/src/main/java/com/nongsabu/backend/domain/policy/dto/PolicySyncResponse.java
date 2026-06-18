package com.nongsabu.backend.domain.policy.dto;

public record PolicySyncResponse(
        int fetchedCount,
        int savedCount,
        int embeddedCount
) {}
