package com.nongsabu.backend.domain.report.dto;

import com.nongsabu.backend.domain.report.entity.AnalysisReport;

public record AnalysisReportResponse(
        Long reportId,
        Long imageId,
        String status,
        String reportText,
        String ragContext,
        String externalMarketContext
) {

    public static AnalysisReportResponse from(AnalysisReport report) {
        return new AnalysisReportResponse(
                report.getId(),
                report.getUploadedImage().getId(),
                report.getStatus().name(),
                report.getReportText(),
                report.getRagContext(),
                report.getExternalMarketContext()
        );
    }
}

