package com.nongsabu.backend.domain.externalapilog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.externalapilog.entity.ExternalApiLog;
import com.nongsabu.backend.domain.externalapilog.repository.ExternalApiLogRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.report.entity.AnalysisReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExternalApiLogService {

    private static final int HTTP_OK = 200;
    private static final int HTTP_INTERNAL_ERROR = 500;

    private final ExternalApiLogRepository externalApiLogRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logKamisMarketSnapshot(
            Long memberId,
            Long analysisReportId,
            String cropName,
            boolean success,
            String errorMessage
    ) {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("cropName", cropName);
        putIfNotBlank(requestParams, "errorMessage", errorMessage);

        save(
                memberId,
                analysisReportId,
                "KAMIS",
                "market-snapshot",
                requestParams,
                success ? HTTP_OK : HTTP_INTERNAL_ERROR,
                success
        );
    }

    @Transactional
    public void logLlmGeneration(
            Long memberId,
            Long analysisReportId,
            String endpoint,
            String prompt,
            String context,
            String response,
            boolean success,
            String errorMessage
    ) {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("promptLength", length(prompt));
        requestParams.put("contextLength", length(context));
        requestParams.put("responsePreview", preview(response));
        putIfNotBlank(requestParams, "errorMessage", errorMessage);

        save(
                memberId,
                analysisReportId,
                "LLM",
                endpoint,
                requestParams,
                success ? HTTP_OK : HTTP_INTERNAL_ERROR,
                success
        );
    }

    public List<Map<String, Object>> getRecentToolCallLogs(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        externalApiLogRepository
                .findByProviderOrderByCreatedAtDesc("TOOL_CALL", PageRequest.of(0, limit))
                .forEach(log -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> params = objectMapper.readValue(log.getRequestParams(), Map.class);
                        entry.put("toolName", params.getOrDefault("toolName", log.getEndpoint()));
                        entry.put("input", params.getOrDefault("input", ""));
                        entry.put("outputPreview", params.getOrDefault("outputPreview", ""));
                    } catch (Exception e) {
                        entry.put("toolName", log.getEndpoint());
                        entry.put("input", "");
                        entry.put("outputPreview", "");
                    }
                    entry.put("success", log.isSuccess());
                    entry.put("createdAt", log.getCreatedAt().toString());
                    result.add(entry);
                });
        return result;
    }

    public void logToolCall(String toolName, String input, String outputPreview) {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("toolName", toolName);
        requestParams.put("input", input == null ? "" : input);
        requestParams.put("outputPreview", preview(outputPreview));
        save(null, null, "TOOL_CALL", toolName, requestParams, HTTP_OK, true);
    }

    @Transactional
    public void logDebugProbe(
            String provider,
            String endpoint,
            Map<String, Object> requestSummary,
            String responsePreview,
            Integer statusCode,
            boolean success,
            String errorMessage,
            long elapsedMillis
    ) {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("requestSummary", requestSummary == null ? Map.of() : requestSummary);
        requestParams.put("responsePreview", preview(responsePreview));
        requestParams.put("elapsedMillis", elapsedMillis);
        putIfNotBlank(requestParams, "errorMessage", errorMessage);

        save(
                null,
                null,
                provider,
                endpoint,
                requestParams,
                statusCode == null ? (success ? HTTP_OK : HTTP_INTERNAL_ERROR) : statusCode,
                success
        );
    }

    private void save(
            Long memberId,
            Long analysisReportId,
            String provider,
            String endpoint,
            Map<String, Object> requestParams,
            Integer statusCode,
            boolean success
    ) {
        try {
            externalApiLogRepository.save(ExternalApiLog.builder()
                    .member(reference(Member.class, memberId))
                    .analysisReport(reference(AnalysisReport.class, analysisReportId))
                    .provider(provider)
                    .endpoint(endpoint)
                    .requestParams(toJson(requestParams))
                    .statusCode(statusCode)
                    .success(success)
                    .build());
        } catch (RuntimeException ignored) {
            // 외부 호출 로그 저장 실패가 사용자 요청 흐름을 막지 않도록 best-effort로 처리한다.
        }
    }

    private <T> T reference(Class<T> entityClass, Long id) {
        return id == null ? null : entityManager.getReference(entityClass, id);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 300 ? value : value.substring(0, 300);
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
