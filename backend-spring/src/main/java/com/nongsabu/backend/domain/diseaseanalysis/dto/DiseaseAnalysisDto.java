package com.nongsabu.backend.domain.diseaseanalysis.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.diseaseanalysis.entity.DiseaseAnalysis;
import com.nongsabu.backend.domain.diseaseanalysis.entity.RiskLevel;

public record DiseaseAnalysisDto(
        Long id,
        Long memberId,
        Long cropImageId,
        String cropName,
        String predictedDisease,
        Double confidence,
        RiskLevel riskLevel,
        String rawResponseJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DiseaseAnalysisDto from(DiseaseAnalysis diseaseAnalysis) {
        return new DiseaseAnalysisDto(
                diseaseAnalysis.getId(),
                diseaseAnalysis.getMember().getId(),
                diseaseAnalysis.getCropImage().getId(),
                diseaseAnalysis.getCropName(),
                diseaseAnalysis.getPredictedDisease(),
                diseaseAnalysis.getConfidence(),
                diseaseAnalysis.getRiskLevel(),
                diseaseAnalysis.getRawResponseJson(),
                diseaseAnalysis.getCreatedAt(),
                diseaseAnalysis.getUpdatedAt()
        );
    }
}
