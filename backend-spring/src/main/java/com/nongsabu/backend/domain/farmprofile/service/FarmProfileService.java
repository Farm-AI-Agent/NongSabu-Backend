package com.nongsabu.backend.domain.farmprofile.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.common.exception.ErrorCode;
import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.farmprofile.dto.FarmProfileDto;
import com.nongsabu.backend.domain.farmprofile.dto.FarmProfileRequest;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import com.nongsabu.backend.domain.farmprofile.repository.FarmProfileRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmProfileService {

    private final FarmProfileRepository farmProfileRepository;
    private final MemberRepository memberRepository;
    private final CropRepository cropRepository;

    @Transactional
    public FarmProfileDto create(Long memberId, FarmProfileRequest request) {
        if (farmProfileRepository.findByMemberId(memberId).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 농장 프로필이 등록되어 있습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Crop mainCrop = getCropOrNull(request.mainCropId());

        FarmProfile farmProfile = farmProfileRepository.save(FarmProfile.builder()
                .member(member)
                .region(request.region())
                .experienceLevel(request.experienceLevel())
                .farmSize(request.farmSize())
                .mainCrop(mainCrop)
                .age(request.age())
                .youngFarmerEligible(request.youngFarmerEligible())
                .farmingStartYear(request.farmingStartYear())
                .farmingType(request.farmingType())
                .residenceRegion(request.residenceRegion())
                .farmlandRegion(request.farmlandRegion())
                .primaryCropName(request.primaryCropName())
                .secondaryCropNames(request.secondaryCropNames())
                .cultivationArea(request.cultivationArea())
                .cultivationType(request.cultivationType())
                .applicantType(request.applicantType())
                .registeredFarmBusiness(request.registeredFarmBusiness())
                .annualSalesRange(request.annualSalesRange())
                .desiredSupportTypes(request.desiredSupportTypes())
                .selfContributionAvailable(request.selfContributionAvailable())
                .receivedPolicyNames(request.receivedPolicyNames())
                .applicationPeriodPreference(request.applicationPeriodPreference())
                .build());
        return FarmProfileDto.from(farmProfile);
    }

    @Transactional(readOnly = true)
    public FarmProfileDto getMine(Long memberId) {
        return FarmProfileDto.from(getOwnedProfile(memberId));
    }

    @Transactional
    public FarmProfileDto update(Long memberId, FarmProfileRequest request) {
        FarmProfile farmProfile = getOwnedProfile(memberId);
        Crop mainCrop = getCropOrNull(request.mainCropId());
        farmProfile.update(
                request.region(),
                request.experienceLevel(),
                request.farmSize(),
                mainCrop,
                request.age(),
                request.youngFarmerEligible(),
                request.farmingStartYear(),
                request.farmingType(),
                request.residenceRegion(),
                request.farmlandRegion(),
                request.primaryCropName(),
                request.secondaryCropNames(),
                request.cultivationArea(),
                request.cultivationType(),
                request.applicantType(),
                request.registeredFarmBusiness(),
                request.annualSalesRange(),
                request.desiredSupportTypes(),
                request.selfContributionAvailable(),
                request.receivedPolicyNames(),
                request.applicationPeriodPreference()
        );
        return FarmProfileDto.from(farmProfile);
    }

    private FarmProfile getOwnedProfile(Long memberId) {
        return farmProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "농장 프로필을 찾을 수 없습니다."));
    }

    private Crop getCropOrNull(Long cropId) {
        if (cropId == null) {
            return null;
        }
        return cropRepository.findById(cropId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "작물을 찾을 수 없습니다."));
    }
}
