package com.nongsabu.backend.domain.user.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.user.dto.UpdateUserProfileRequest;
import com.nongsabu.backend.domain.user.dto.UserProfileResponse;
import com.nongsabu.backend.domain.user.service.UserService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok("사용자 프로필 조회에 성공했습니다.", userService.getProfile(principal.id()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.ok("사용자 프로필이 수정되었습니다.", userService.updateProfile(principal.id(), request));
    }
}
