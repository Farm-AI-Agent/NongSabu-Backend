package com.nongsabu.backend.infra.mcp;

import com.nongsabu.backend.domain.toolcalllog.service.ToolCallLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StubMcpToolClient implements McpToolClient {

    private final ToolCallLogService toolCallLogService;

    @Override
    public String invoke(String toolName, String payload) {
        try {
            String response = "MCP stub response for tool=%s".formatted(toolName);
            toolCallLogService.log(null, toolName, payload, "{\"response\":\"%s\"}".formatted(response), true);
            return response;
        } catch (RuntimeException exception) {
            toolCallLogService.log(null, toolName, payload, "{\"error\":\"%s\"}".formatted(exception.getMessage()), false);
            throw exception;
        }
    }
}
