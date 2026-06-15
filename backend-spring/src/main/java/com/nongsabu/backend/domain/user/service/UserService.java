package com.nongsabu.backend.domain.user.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.user.dto.UpdateUserProfileRequest;
import com.nongsabu.backend.domain.user.dto.UserProfileResponse;
import com.nongsabu.backend.domain.user.entity.User;
import com.nongsabu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(getUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getUser(userId);
        user.updateProfile(request.fullName(), request.phoneNumber(), request.region());
        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}

