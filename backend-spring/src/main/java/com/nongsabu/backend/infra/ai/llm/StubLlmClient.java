package com.nongsabu.backend.infra.ai.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubLlmClient implements LlmClient {

    @Override
    public String generate(String prompt, String context) {
        return "";
    }
}
