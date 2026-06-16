package com.nongsabu.backend.domain.image.dto;

import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;

public record AnalysisResponse(
        Long imageId,
        Long cropId,
        String cropName,
        String filename,
        String status,
        String diseaseName,
        double confidence,
        String severity,
        String summary,
        String recommendation
) {

    public static AnalysisResponse of(UploadedImage image, ImageAnalysisResult result) {
        return new AnalysisResponse(
                image.getId(),
                image.getCrop() == null ? null : image.getCrop().getId(),
                image.getCrop() == null ? null : image.getCrop().getName(),
                image.getOriginalFilename(),
                image.getAnalysisStatus().name(),
                result == null ? null : result.getDiseaseName(),
                result == null ? 0.0 : result.getConfidence(),
                result == null ? null : result.getSeverity(),
                result == null ? null : result.getSummary(),
                result == null ? null : result.getRecommendation()
        );
    }
}
