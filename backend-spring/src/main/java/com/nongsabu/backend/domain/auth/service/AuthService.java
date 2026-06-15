package com.nongsabu.backend.domain.auth.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.common.exception.ErrorCode;
import com.nongsabu.backend.domain.auth.dto.LoginRequest;
import com.nongsabu.backend.domain.auth.dto.LoginResponse;
import com.nongsabu.backend.domain.auth.dto.SignupRequest;
import com.nongsabu.backend.domain.auth.dto.SignupResponse;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.entity.MemberRole;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import com.nongsabu.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = memberRepository.save(Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(MemberRole.USER)
                .build());

        return new SignupResponse(member.getId(), member.getEmail(), member.getName());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return new LoginResponse(
                jwtTokenProvider.generateToken(String.valueOf(member.getId())),
                "Bearer",
                jwtTokenProvider.getExpirationSeconds()
        );
    }
}

