package com.nongsabu.backend.domain.policy.dto;

import com.nongsabu.backend.domain.policy.entity.PolicySupport;

public record PolicySupportResponse(
        Long id,
        String source,
        String externalId,
        String title,
        String summary,
        String targetGroup,
        String region,
        String category,
        String applicationPeriod,
        String applyMethod,
        String department,
        String contact,
        String detailUrl,
        Double score
) {

    public static PolicySupportResponse from(PolicySupport policy) {
        return from(policy, null);
    }

    public static PolicySupportResponse from(PolicySupport policy, Double score) {
        return new PolicySupportResponse(
                policy.getId(),
                policy.getSource(),
                policy.getExternalId(),
                policy.getTitle(),
                policy.getSummary(),
                policy.getTargetGroup(),
                policy.getRegion(),
                policy.getCategory(),
                policy.getApplicationPeriod(),
                policy.getApplyMethod(),
                policy.getDepartment(),
                policy.getContact(),
                policy.getDetailUrl(),
                score
        );
    }
}
