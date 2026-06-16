package com.nongsabu.backend.domain.externalapilog.dto;

import java.time.LocalDateTime;
import com.nongsabu.backend.domain.externalapilog.entity.ExternalApiLog;

public record ExternalApiLogDto(
        Long id,
        Long memberId,
        Long analysisReportId,
        String provider,
        String endpoint,
        String requestParams,
        Integer statusCode,
        boolean success,
        LocalDateTime createdAt
) {

    public static ExternalApiLogDto from(ExternalApiLog log) {
        return new ExternalApiLogDto(
                log.getId(),
                log.getMember() == null ? null : log.getMember().getId(),
                log.getAnalysisReport() == null ? null : log.getAnalysisReport().getId(),
                log.getProvider(),
                log.getEndpoint(),
                log.getRequestParams(),
                log.getStatusCode(),
                log.isSuccess(),
                log.getCreatedAt()
        );
    }
}

