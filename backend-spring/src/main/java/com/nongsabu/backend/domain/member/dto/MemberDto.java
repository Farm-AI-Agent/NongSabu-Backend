package com.nongsabu.backend.domain.member.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.entity.MemberRole;

public record MemberDto(
        Long id,
        String email,
        String name,
        MemberRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MemberDto from(Member member) {
        return new MemberDto(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}

