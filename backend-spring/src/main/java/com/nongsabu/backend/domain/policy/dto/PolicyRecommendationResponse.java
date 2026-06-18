package com.nongsabu.backend.domain.policy.dto;

import java.util.List;

public record PolicyRecommendationResponse(
        String query,
        List<PolicySupportResponse> items
) {}
