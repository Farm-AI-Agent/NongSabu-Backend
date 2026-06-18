package com.nongsabu.backend.domain.policy.dto;

import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PolicyRecommendationRequest(
        String query,
        String region,
        String cropName,
        ExperienceLevel experienceLevel,
        String farmSize,
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
        @Min(1) @Max(20) Integer topK,
        Boolean includeProfile
) {

    public int safeTopK() {
        return topK == null ? 5 : topK;
    }

    public boolean shouldIncludeProfile() {
        return includeProfile == null || includeProfile;
    }
}
