package com.nongsabu.backend.domain.policy.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PolicyPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<PolicySupportResponse> items
) {

    public static PolicyPageResponse from(Page<PolicySupportResponse> page) {
        return new PolicyPageResponse(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent()
        );
    }
}
