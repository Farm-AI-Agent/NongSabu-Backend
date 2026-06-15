package com.nongsabu.backend.domain.farmprofile.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
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

    @Column(name = "main_crop", length = 100)
    private String mainCrop;

    public void update(String region, ExperienceLevel experienceLevel, String farmSize, String mainCrop) {
        this.region = region;
        this.experienceLevel = experienceLevel;
        this.farmSize = farmSize;
        this.mainCrop = mainCrop;
    }
}
