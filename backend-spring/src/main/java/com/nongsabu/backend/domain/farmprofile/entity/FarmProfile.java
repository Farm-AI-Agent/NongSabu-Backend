package com.nongsabu.backend.domain.farmprofile.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "farm_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FarmProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 30)
    private ExperienceLevel experienceLevel;

    @Column(name = "farm_size", length = 100)
    private String farmSize;

    // 대표 작물은 문자열이 아니라 CROP 마스터와 연결해 정책 추천과 리포트 개인화에 재사용한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_crop_id")
    private Crop mainCrop;

    @Column(name = "age")
    private Integer age;

    @Column(name = "young_farmer_eligible")
    private Boolean youngFarmerEligible;

    @Column(name = "farming_start_year")
    private Integer farmingStartYear;

    @Column(name = "farming_type", length = 50)
    private String farmingType;

    @Column(name = "residence_region", length = 120)
    private String residenceRegion;

    @Column(name = "farmland_region", length = 120)
    private String farmlandRegion;

    @Column(name = "primary_crop_name", length = 100)
    private String primaryCropName;

    @Column(name = "secondary_crop_names", length = 500)
    private String secondaryCropNames;

    @Column(name = "cultivation_area", length = 100)
    private String cultivationArea;

    @Column(name = "cultivation_type", length = 50)
    private String cultivationType;

    @Column(name = "applicant_type", length = 50)
    private String applicantType;

    @Column(name = "registered_farm_business")
    private Boolean registeredFarmBusiness;

    @Column(name = "annual_sales_range", length = 100)
    private String annualSalesRange;

    @Column(name = "desired_support_types", length = 500)
    private String desiredSupportTypes;

    @Column(name = "self_contribution_available")
    private Boolean selfContributionAvailable;

    @Column(name = "received_policy_names", length = 1000)
    private String receivedPolicyNames;

    @Column(name = "application_period_preference", length = 100)
    private String applicationPeriodPreference;

    public void update(
            String region,
            ExperienceLevel experienceLevel,
            String farmSize,
            Crop mainCrop,
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
        this.region = region;
        this.experienceLevel = experienceLevel;
        this.farmSize = farmSize;
        this.mainCrop = mainCrop;
        this.age = age;
        this.youngFarmerEligible = youngFarmerEligible;
        this.farmingStartYear = farmingStartYear;
        this.farmingType = farmingType;
        this.residenceRegion = residenceRegion;
        this.farmlandRegion = farmlandRegion;
        this.primaryCropName = primaryCropName;
        this.secondaryCropNames = secondaryCropNames;
        this.cultivationArea = cultivationArea;
        this.cultivationType = cultivationType;
        this.applicantType = applicantType;
        this.registeredFarmBusiness = registeredFarmBusiness;
        this.annualSalesRange = annualSalesRange;
        this.desiredSupportTypes = desiredSupportTypes;
        this.selfContributionAvailable = selfContributionAvailable;
        this.receivedPolicyNames = receivedPolicyNames;
        this.applicationPeriodPreference = applicationPeriodPreference;
    }
}
