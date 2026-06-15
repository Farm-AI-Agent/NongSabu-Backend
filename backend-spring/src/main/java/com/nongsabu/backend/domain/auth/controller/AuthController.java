package com.nongsabu.backend.domain.auth.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.auth.dto.LoginRequest;
import com.nongsabu.backend.domain.auth.dto.LoginResponse;
import com.nongsabu.backend.domain.auth.dto.SignupRequest;
import com.nongsabu.backend.domain.auth.dto.SignupResponse;
import com.nongsabu.backend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
