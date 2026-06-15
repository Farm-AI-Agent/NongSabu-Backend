package com.nongsabu.backend.infra.mcp;

import org.springframework.stereotype.Component;

@Component
public class StubMcpToolClient implements McpToolClient {

    @Override
    public String invoke(String toolName, String payload) {
        return "MCP stub response for tool=%s".formatted(toolName);
    }
}

