package com.nongsabu.backend.infra.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.domain.toolcalllog.service.ToolCallLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StubMcpToolClientTest {

    @Mock
    private ToolCallLogService toolCallLogService;

    @Test
    void invokeStoresToolCallLog() {
        StubMcpToolClient client = new StubMcpToolClient(toolCallLogService);

        String response = client.invoke("policy-search", "{\"region\":\"나주\"}");

        assertThat(response).contains("policy-search");
        verify(toolCallLogService).log(
                isNull(),
                eq("policy-search"),
                eq("{\"region\":\"나주\"}"),
                contains("policy-search"),
                eq(true)
        );
    }
}
