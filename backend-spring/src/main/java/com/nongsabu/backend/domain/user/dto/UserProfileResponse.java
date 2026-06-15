package com.nongsabu.backend.domain.user.dto;

import com.nongsabu.backend.domain.user.entity.User;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String region
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRegion()
        );
    }
}

