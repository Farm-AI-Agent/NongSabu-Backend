package com.nongsabu.backend.domain.analysisreport.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.analysisreport.entity.AnalysisReport;

public record AnalysisReportDto(
        Long id,
        Long memberId,
        Long diseaseAnalysisId,
        String title,
        String summary,
        String suspectedProblem,
        String recommendedActions,
        String checklist,
        String ragReferencesJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AnalysisReportDto from(AnalysisReport report) {
        return new AnalysisReportDto(
                report.getId(),
                report.getMember().getId(),
                report.getDiseaseAnalysis() == null ? null : report.getDiseaseAnalysis().getId(),
                report.getTitle(),
                report.getSummary(),
                report.getSuspectedProblem(),
                report.getRecommendedActions(),
                report.getChecklist(),
                report.getRagReferencesJson(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}

