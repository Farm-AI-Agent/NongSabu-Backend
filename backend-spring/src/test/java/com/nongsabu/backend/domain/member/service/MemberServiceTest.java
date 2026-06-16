package com.nongsabu.backend.domain.member.service;

import static com.nongsabu.backend.support.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.member.dto.MemberDto;
import com.nongsabu.backend.domain.member.dto.UpdateMemberProfileRequest;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void getMyInfoReturnsCurrentMember() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));

        MemberDto response = memberService.getMyInfo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user1@example.com");
    }

    @Test
    void updateMyInfoChangesName() {
        var member = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberDto response = memberService.updateMyInfo(1L, new UpdateMemberProfileRequest("updated-name"));

        assertThat(response.name()).isEqualTo("updated-name");
        assertThat(member.getName()).isEqualTo("updated-name");
    }

    @Test
    void getMemberFailsWhenMemberDoesNotExist() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(99L))
                .isInstanceOf(BusinessException.class);
    }
}
