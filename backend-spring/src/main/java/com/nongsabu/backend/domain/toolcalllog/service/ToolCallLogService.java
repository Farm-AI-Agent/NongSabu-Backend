package com.nongsabu.backend.domain.toolcalllog.service;

import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.toolcalllog.entity.ToolCallLog;
import com.nongsabu.backend.domain.toolcalllog.repository.ToolCallLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToolCallLogService {

    private final ToolCallLogRepository toolCallLogRepository;
    private final EntityManager entityManager;

    @Transactional
    public void log(Long memberId, String toolName, String requestPayload, String responsePayload, boolean success) {
        try {
            toolCallLogRepository.save(ToolCallLog.builder()
                    .member(memberId == null ? null : entityManager.getReference(Member.class, memberId))
                    .toolName(toolName)
                    .requestPayload(normalizeJson(requestPayload))
                    .responsePayload(normalizeJson(responsePayload))
                    .success(success)
                    .build());
        } catch (RuntimeException ignored) {
            // MCP/Tool Calling 로그는 추적용이므로 저장 실패가 실제 도구 호출 흐름을 막지 않게 둔다.
        }
    }

    private String normalizeJson(String payload) {
        return payload == null || payload.isBlank() ? "{}" : payload;
    }
}
