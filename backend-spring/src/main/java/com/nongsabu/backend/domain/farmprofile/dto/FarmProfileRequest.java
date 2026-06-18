package com.nongsabu.backend.domain.farmprofile.dto;

import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;

public record FarmProfileRequest(
        String region,
        ExperienceLevel experienceLevel,
        String farmSize,
        Long mainCropId,
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
        String applicationPeriodPreference
) {

    public FarmProfileRequest(String region, ExperienceLevel experienceLevel, String farmSize, Long mainCropId) {
        this(
                region,
                experienceLevel,
                farmSize,
                mainCropId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
