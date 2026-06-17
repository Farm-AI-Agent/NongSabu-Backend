package com.nongsabu.backend.domain.agri.dto;

import java.util.List;

// youngV2/policyListV2 API 응답 구조:
// {"policy_paging":{"currentPage":N,"totalCount":N,"lastPage":N,...},"policy_list":[{...}]}
public record YoungFarmerResponse(
        int page,
        int totalCount,
        int lastPage,
        List<Item> items
) {
    public record Item(
            String seq,
            String title,
            String summary,
            String applStDt,
            String applEdDt,
            String area1Nm,
            String chargeAgency,
            String chargeTel,
            String infoUrl
    ) {}
}
