package com.nongsabu.backend.domain.farm.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.farm.dto.FarmRequest;
import com.nongsabu.backend.domain.farm.dto.FarmResponse;
import com.nongsabu.backend.domain.farm.service.FarmService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    @PostMapping
    public ApiResponse<FarmResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody FarmRequest request
    ) {
        return ApiResponse.ok("농장 정보를 등록했습니다.", farmService.createFarm(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<FarmResponse>> getAll(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok("농장 목록을 조회했습니다.", farmService.getFarms(principal.id()));
    }

    @GetMapping("/me")
    public ApiResponse<FarmResponse> getMine(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok("내 농장 정보를 조회했습니다.", farmService.getPrimaryFarm(principal.id()));
    }

    @GetMapping("/{farmId}")
    public ApiResponse<FarmResponse> getOne(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long farmId
    ) {
        return ApiResponse.ok("농장 정보를 조회했습니다.", farmService.getFarm(principal.id(), farmId));
    }

    @PutMapping("/{farmId}")
    public ApiResponse<FarmResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long farmId,
            @Valid @RequestBody FarmRequest request
    ) {
        return ApiResponse.ok("농장 정보를 수정했습니다.", farmService.updateFarm(principal.id(), farmId, request));
    }

    @PutMapping("/me")
    public ApiResponse<FarmResponse> upsertMine(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody FarmRequest request
    ) {
        return ApiResponse.ok("내 농장 정보를 저장했습니다.", farmService.upsertPrimaryFarm(principal.id(), request));
    }
}
