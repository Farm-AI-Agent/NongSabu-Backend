package com.nongsabu.backend.domain.auth.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.auth.dto.LoginRequest;
import com.nongsabu.backend.domain.auth.dto.SignupRequest;
import com.nongsabu.backend.domain.auth.dto.TokenResponse;
import com.nongsabu.backend.domain.user.entity.User;
import com.nongsabu.backend.domain.user.entity.UserRole;
import com.nongsabu.backend.domain.user.repository.UserRepository;
import com.nongsabu.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .region(request.region())
                .role(UserRole.USER)
                .build());

        return new TokenResponse(jwtTokenProvider.generateToken(String.valueOf(user.getId())), "Bearer");
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        return new TokenResponse(jwtTokenProvider.generateToken(String.valueOf(user.getId())), "Bearer");
    }
}

