package com.nongsabu.backend.domain.member.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.member.dto.MemberDto;
import com.nongsabu.backend.domain.member.dto.UpdateMemberProfileRequest;
import com.nongsabu.backend.domain.member.service.MemberService;
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
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ApiResponse.ok(memberService.getMyInfo(customUserDetails.id()));
    }

    @PutMapping("/me")
    public ApiResponse<MemberDto> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        return ApiResponse.ok(memberService.updateMyInfo(customUserDetails.id(), request));
    }
}
