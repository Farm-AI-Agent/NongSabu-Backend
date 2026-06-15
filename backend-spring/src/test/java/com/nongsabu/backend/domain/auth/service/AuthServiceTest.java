package com.nongsabu.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("signup success")
    void signupSuccess() {
        SignupRequest request = new SignupRequest("user@example.com", "password1234", "HongGilDong");
        Member savedMember = Member.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .name("HongGilDong")
                .role(MemberRole.USER)
                .build();

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        SignupResponse response = authService.signup(request);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("HongGilDong");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("signup fails when email is duplicated")
    void signupFailWhenDuplicateEmail() {
        SignupRequest request = new SignupRequest("user@example.com", "password1234", "HongGilDong");
        given(memberRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("login success")
    void loginSuccess() {
        LoginRequest request = new LoginRequest("user@example.com", "password1234");
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .name("HongGilDong")
                .role(MemberRole.USER)
                .build();

        given(memberRepository.findByEmail(request.email())).willReturn(java.util.Optional.of(member));
        given(passwordEncoder.matches(request.password(), member.getPassword())).willReturn(true);
        given(jwtTokenProvider.generateToken("1")).willReturn("access-token");
        given(jwtTokenProvider.getExpirationSeconds()).willReturn(3600L);

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("login fails when password is invalid")
    void loginFailWhenPasswordMismatch() {
        LoginRequest request = new LoginRequest("user@example.com", "password1234");
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .name("HongGilDong")
                .role(MemberRole.USER)
                .build();

        given(memberRepository.findByEmail(request.email())).willReturn(java.util.Optional.of(member));
        given(passwordEncoder.matches(request.password(), member.getPassword())).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }
}
