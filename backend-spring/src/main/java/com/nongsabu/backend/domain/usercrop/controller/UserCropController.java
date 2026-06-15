package com.nongsabu.backend.domain.usercrop.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.usercrop.dto.UserCropDto;
import com.nongsabu.backend.domain.usercrop.dto.UserCropRequest;
import com.nongsabu.backend.domain.usercrop.service.UserCropService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-crops")
@RequiredArgsConstructor
public class UserCropController {

    private final UserCropService userCropService;

    @PostMapping
    public ApiResponse<UserCropDto> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserCropRequest request
    ) {
        return ApiResponse.ok(userCropService.create(principal.id(), request));
    }

    @GetMapping("/me")
    public ApiResponse<List<UserCropDto>> getMine(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(userCropService.getMine(principal.id()));
    }

    @DeleteMapping("/{userCropId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long userCropId
    ) {
        userCropService.delete(principal.id(), userCropId);
        return ApiResponse.ok(null);
    }
}
