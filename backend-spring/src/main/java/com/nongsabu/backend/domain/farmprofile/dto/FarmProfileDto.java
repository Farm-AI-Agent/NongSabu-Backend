package com.nongsabu.backend.domain.farmprofile.dto;

import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import java.time.LocalDateTime;

public record FarmProfileDto(
        Long id,
        Long memberId,
        String region,
        ExperienceLevel experienceLevel,
        String farmSize,
        Long mainCropId,
        String mainCropName,
        Integer age,
        Boolean youngFarmerEligible,
        Integer farmingStartYear,
        String farmingType,
        String residenceRegion,
        String farmlandRegion,
        String primaryCropName,
        String secondaryCropNames,
        String cultivationArea,
        String cultivationType,
        String applicantType,
        Boolean registeredFarmBusiness,
        String annualSalesRange,
        String desiredSupportTypes,
        Boolean selfContributionAvailable,
        String receivedPolicyNames,
        String applicationPeriodPreference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FarmProfileDto from(FarmProfile farmProfile) {
        return new FarmProfileDto(
                farmProfile.getId(),
                farmProfile.getMember().getId(),
                farmProfile.getRegion(),
                farmProfile.getExperienceLevel(),
                farmProfile.getFarmSize(),
                farmProfile.getMainCrop() == null ? null : farmProfile.getMainCrop().getId(),
                farmProfile.getMainCrop() == null ? null : farmProfile.getMainCrop().getName(),
                farmProfile.getAge(),
                farmProfile.getYoungFarmerEligible(),
                farmProfile.getFarmingStartYear(),
                farmProfile.getFarmingType(),
                farmProfile.getResidenceRegion(),
                farmProfile.getFarmlandRegion(),
                farmProfile.getPrimaryCropName(),
                farmProfile.getSecondaryCropNames(),
                farmProfile.getCultivationArea(),
                farmProfile.getCultivationType(),
                farmProfile.getApplicantType(),
                farmProfile.getRegisteredFarmBusiness(),
                farmProfile.getAnnualSalesRange(),
                farmProfile.getDesiredSupportTypes(),
                farmProfile.getSelfContributionAvailable(),
                farmProfile.getReceivedPolicyNames(),
                farmProfile.getApplicationPeriodPreference(),
                farmProfile.getCreatedAt(),
                farmProfile.getUpdatedAt()
        );
    }
}
