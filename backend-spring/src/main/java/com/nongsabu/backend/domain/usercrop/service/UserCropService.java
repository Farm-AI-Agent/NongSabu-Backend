package com.nongsabu.backend.domain.usercrop.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.common.exception.ErrorCode;
import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import com.nongsabu.backend.domain.usercrop.dto.UserCropDto;
import com.nongsabu.backend.domain.usercrop.dto.UserCropRequest;
import com.nongsabu.backend.domain.usercrop.entity.UserCrop;
import com.nongsabu.backend.domain.usercrop.repository.UserCropRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCropService {

    private final UserCropRepository userCropRepository;
    private final CropRepository cropRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public UserCropDto create(Long memberId, UserCropRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "작물을 찾을 수 없습니다."));

        UserCrop userCrop = userCropRepository.save(UserCrop.builder()
                .member(member)
                .crop(crop)
                .cultivationArea(request.cultivationArea())
                .memo(request.memo())
                .build());
        return UserCropDto.from(userCrop);
    }

    @Transactional(readOnly = true)
    public List<UserCropDto> getMine(Long memberId) {
        return userCropRepository.findAllByMemberId(memberId).stream()
                .map(UserCropDto::from)
                .toList();
    }

    @Transactional
    public void delete(Long memberId, Long userCropId) {
        UserCrop userCrop = userCropRepository.findByIdAndMemberId(userCropId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "재배 작물을 찾을 수 없습니다."));
        userCropRepository.delete(userCrop);
    }
}
