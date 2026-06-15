package com.nongsabu.backend.domain.member.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.common.exception.ErrorCode;
import com.nongsabu.backend.domain.member.dto.MemberDto;
import com.nongsabu.backend.domain.member.dto.UpdateMemberProfileRequest;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MemberDto getMyInfo(Long memberId) {
        return MemberDto.from(getMember(memberId));
    }

    @Transactional
    public MemberDto updateMyInfo(Long memberId, UpdateMemberProfileRequest request) {
        Member member = getMember(memberId);
        member.updateName(request.name());
        return MemberDto.from(member);
    }

    @Transactional(readOnly = true)
    public Member getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return member;
    }
}
