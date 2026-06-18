package com.nongsabu.backend.domain.policy.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.policy.dto.PolicyPageResponse;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationRequest;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationResponse;
import com.nongsabu.backend.domain.policy.dto.PolicySupportResponse;
import com.nongsabu.backend.domain.policy.dto.PolicySyncRequest;
import com.nongsabu.backend.domain.policy.dto.PolicySyncResponse;
import com.nongsabu.backend.domain.policy.service.PolicySupportService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support-programs")
@RequiredArgsConstructor
public class SupportProgramController {

    private final PolicySupportService policySupportService;

    @Value("${app.policy.admin-password:policy-admin-local}")
    private String adminPassword;

    @GetMapping
    public ApiResponse<PolicyPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cropId
    ) {
        return ApiResponse.ok(
                "지원사업 목록을 조회했습니다.",
                policySupportService.search(page, size, source, region, category, keyword)
        );
    }

    @GetMapping("/recommended")
    public ApiResponse<PolicyRecommendationResponse> recommended(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "5") Integer topK
    ) {
        PolicyRecommendationRequest request = new PolicyRecommendationRequest(
                query,
                region,
                cropName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                category,
                null,
                null,
                null,
                topK,
                true
        );
        Long memberId = principal == null ? null : principal.id();
        return ApiResponse.ok("추천 지원사업을 조회했습니다.", policySupportService.recommend(memberId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PolicySupportResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok("지원사업 상세를 조회했습니다.", policySupportService.get(id));
    }

    @PostMapping("/reindex")
    public ApiResponse<PolicySyncResponse> reindex(
            @RequestHeader(name = "X-Policy-Admin-Password", required = false) String password,
            @Valid @RequestBody(required = false) PolicySyncRequest request
    ) {
        verifyAdminPassword(password);
        PolicySyncRequest syncRequest = request == null
                ? new PolicySyncRequest(true, true, null, 1, 50, 5)
                : request;
        return ApiResponse.ok("지원사업 재색인을 완료했습니다.", policySupportService.syncAll(syncRequest));
    }

    private void verifyAdminPassword(String password) {
        if (password == null || password.isBlank() || !password.equals(adminPassword)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid policy admin password.");
        }
    }
}
