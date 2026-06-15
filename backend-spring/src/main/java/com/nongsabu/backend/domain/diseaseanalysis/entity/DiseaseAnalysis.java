package com.nongsabu.backend.domain.diseaseanalysis.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
import com.nongsabu.backend.domain.cropimage.entity.CropImage;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Consider soft delete if analysis history must be retained after user data deletion.
@Getter
@Builder
@Entity
@Table(name = "disease_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DiseaseAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_image_id", nullable = false)
    private CropImage cropImage;

    @Column(name = "crop_name", length = 100)
    private String cropName;

    @Column(name = "predicted_disease", length = 200)
    private String predictedDisease;

    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel;

    @Column(name = "raw_response_json", columnDefinition = "jsonb")
    private String rawResponseJson;
}

