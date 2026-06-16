package com.nongsabu.backend.domain.image.entity;

import com.nongsabu.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Canonical storage for crop image disease prediction results.
 */
@Getter
@Builder
@Entity
@Table(name = "image_analysis_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ImageAnalysisResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_image_id", unique = true)
    private UploadedImage uploadedImage;

    @Column(length = 120)
    private String diseaseName;

    private double confidence;

    @Column(length = 50)
    private String severity;

    @Column(length = 1000)
    private String summary;

    @Column(length = 2000)
    private String recommendation;

    @Column(columnDefinition = "text")
    private String rawResponse;
}
