package com.nongsabu.backend.infra.mcp;

public interface McpToolClient {

    String invoke(String toolName, String payload);
}

