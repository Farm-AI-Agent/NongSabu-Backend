package com.nongsabu.backend.domain.farmprofile.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.farmprofile.dto.FarmProfileDto;
import com.nongsabu.backend.domain.farmprofile.dto.FarmProfileRequest;
import com.nongsabu.backend.domain.farmprofile.service.FarmProfileService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farm-profiles")
@RequiredArgsConstructor
public class FarmProfileController {

    private final FarmProfileService farmProfileService;

    @PostMapping("/me")
    public ApiResponse<FarmProfileDto> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody FarmProfileRequest request
    ) {
        return ApiResponse.ok(farmProfileService.create(principal.id(), request));
    }

    @GetMapping("/me")
    public ApiResponse<FarmProfileDto> getMine(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(farmProfileService.getMine(principal.id()));
    }

    @PutMapping("/me")
    public ApiResponse<FarmProfileDto> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody FarmProfileRequest request
    ) {
        return ApiResponse.ok(farmProfileService.update(principal.id(), request));
    }
}
