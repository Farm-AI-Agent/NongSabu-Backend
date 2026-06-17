package com.nongsabu.backend.domain.agri.dto;

import java.util.List;

// GOV24 serviceList API 응답 구조:
// {"currentCount":N,"data":[{"서비스ID":"...","서비스명":"...","서비스목적요약":"...",...}],"totalCount":N,"page":N,"perPage":N}
public record Gov24Response(
        int page,
        int perPage,
        int totalCount,
        List<Item> items
) {
    public record Item(
            String serviceId,
            String serviceName,
            String servicePurpose,
            String targetGroup,
            String serviceField,
            String applyDeadline,
            String applyMethod,
            String department,
            String detailUrl
    ) {}
}
