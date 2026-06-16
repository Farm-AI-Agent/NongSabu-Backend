package com.nongsabu.backend.domain.image.dto;

import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.AnalysisStatus;
import com.nongsabu.backend.domain.image.entity.UploadedImage;

public record AnalysisResponse(
        Long imageId,
        Long cropId,
        String cropName,
        String filename,
        String status,
        boolean supported,
        String message,
        String diseaseName,
        double confidence,
        String severity,
        String summary,
        String recommendation
) {

    public static AnalysisResponse of(UploadedImage image, ImageAnalysisResult result) {
        AnalysisStatus status = image.getAnalysisStatus();
        return new AnalysisResponse(
                image.getId(),
                image.getCrop() == null ? null : image.getCrop().getId(),
                image.getCrop() == null ? null : image.getCrop().getName(),
                image.getOriginalFilename(),
                status.name(),
                status != AnalysisStatus.UNSUPPORTED,
                resolveMessage(status, result),
                result == null ? null : result.getDiseaseName(),
                result == null ? 0.0 : result.getConfidence(),
                status == AnalysisStatus.UNSUPPORTED || result == null ? null : result.getSeverity(),
                result == null ? null : result.getSummary(),
                result == null ? null : result.getRecommendation()
        );
    }

    private static String resolveMessage(AnalysisStatus status, ImageAnalysisResult result) {
        if (status == AnalysisStatus.UNSUPPORTED && result != null && result.getSummary() != null) {
            return result.getSummary();
        }
        return switch (status) {
            case PENDING -> "이미지 분석 대기 중입니다.";
            case PROCESSING -> "이미지 분석을 진행 중입니다.";
            case COMPLETED -> "이미지 분석이 완료되었습니다.";
            case UNSUPPORTED -> "현재 MVP에서는 포도 병충해 분석만 지원합니다.";
            case FAILED -> "이미지 분석에 실패했습니다.";
        };
    }
}
