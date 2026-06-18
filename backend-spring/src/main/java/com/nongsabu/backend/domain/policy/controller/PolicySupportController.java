package com.nongsabu.backend.domain.policy.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.policy.dto.PolicyEmbeddingStatusResponse;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationRequest;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationResponse;
import com.nongsabu.backend.domain.policy.dto.PolicyPageResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicySupportController {

    private final PolicySupportService policySupportService;

    @Value("${app.policy.admin-password:policy-admin-local}")
    private String adminPassword;

    @PostMapping("/sync")
    public ApiResponse<PolicySyncResponse> sync(@Valid @RequestBody PolicySyncRequest request) {
        return ApiResponse.ok("Policy support sync completed.", policySupportService.sync(request));
    }

    @PostMapping("/sync-all")
    public ApiResponse<PolicySyncResponse> syncAll(@Valid @RequestBody PolicySyncRequest request) {
        return ApiResponse.ok("Policy support batch sync completed.", policySupportService.syncAll(request));
    }

    @PostMapping("/admin/sync")
    public ApiResponse<PolicySyncResponse> adminSync(
            @RequestHeader(name = "X-Policy-Admin-Password", required = false) String password,
            @Valid @RequestBody PolicySyncRequest request
    ) {
        verifyAdminPassword(password);
        return ApiResponse.ok("Policy support sync completed.", policySupportService.sync(request));
    }

    @PostMapping("/admin/sync-all")
    public ApiResponse<PolicySyncResponse> adminSyncAll(
            @RequestHeader(name = "X-Policy-Admin-Password", required = false) String password,
            @Valid @RequestBody PolicySyncRequest request
    ) {
        verifyAdminPassword(password);
        return ApiResponse.ok("Policy support batch sync completed.", policySupportService.syncAll(request));
    }

    @GetMapping("/admin/embedding-status")
    public ApiResponse<PolicyEmbeddingStatusResponse> adminEmbeddingStatus(
            @RequestHeader(name = "X-Policy-Admin-Password", required = false) String password
    ) {
        verifyAdminPassword(password);
        return ApiResponse.ok("Policy embedding status loaded.", policySupportService.embeddingStatus());
    }

    @GetMapping("/admin")
    public ApiResponse<PolicyPageResponse> adminList(
            @RequestHeader(name = "X-Policy-Admin-Password", required = false) String password,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        verifyAdminPassword(password);
        return ApiResponse.ok(
                "Policy support list loaded.",
                policySupportService.search(page, size, source, region, category, keyword)
        );
    }

    @GetMapping
    public ApiResponse<PolicyPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(
                "Policy support list loaded.",
                policySupportService.search(page, size, source, region, category, keyword)
        );
    }

    @PostMapping("/recommendations")
    public ApiResponse<PolicyRecommendationResponse> recommend(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PolicyRecommendationRequest request
    ) {
        return ApiResponse.ok(
                "Policy recommendations loaded.",
                policySupportService.recommend(principal.id(), request)
        );
    }

    @PostMapping("/admin/recommendations")
    public ApiResponse<PolicyRecommendationResponse> adminRecommend(
            @RequestHeader(name = "X-Policy-Admin-Password", required = false) String password,
            @Valid @RequestBody PolicyRecommendationRequest request
    ) {
        verifyAdminPassword(password);
        return ApiResponse.ok(
                "Policy recommendations loaded.",
                policySupportService.recommend(null, request)
        );
    }

    private void verifyAdminPassword(String password) {
        if (password == null || password.isBlank() || !password.equals(adminPassword)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid policy admin password.");
        }
    }
}
