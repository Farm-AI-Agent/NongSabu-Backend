package com.nongsabu.backend.domain.report.dto;

import com.nongsabu.backend.domain.report.entity.AnalysisReport;

public record AnalysisReportResponse(
        Long reportId,
        Long imageId,
        String status,
        String reportText,
        String ragContext,
        String externalMarketContext,
        String diseaseName,
        DiseaseGuidanceResponse diseaseGuidance
) {

    public static AnalysisReportResponse from(AnalysisReport report) {
        return from(report, null, null);
    }

    public static AnalysisReportResponse from(
            AnalysisReport report,
            String diseaseName,
            DiseaseGuidanceResponse diseaseGuidance
    ) {
        return new AnalysisReportResponse(
                report.getId(),
                report.getUploadedImage().getId(),
                report.getStatus().name(),
                report.getReportText(),
                report.getRagContext(),
                report.getExternalMarketContext(),
                diseaseName,
                diseaseGuidance
        );
    }

    public record DiseaseGuidanceResponse(
            String diseaseName,
            String diseaseInfo,
            String outbreakCause,
            String treatment,
            String ragContext
    ) {
    }
}
