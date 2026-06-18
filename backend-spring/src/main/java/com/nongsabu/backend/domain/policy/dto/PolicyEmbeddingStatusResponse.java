package com.nongsabu.backend.domain.policy.dto;

public record PolicyEmbeddingStatusResponse(
        long policyCount,
        long embeddingSyncedCount,
        long embeddingPendingCount,
        boolean vectorSearchReady,
        int vectorSearchResultCount
) {}
