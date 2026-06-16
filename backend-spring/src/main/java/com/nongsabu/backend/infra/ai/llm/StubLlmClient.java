package com.nongsabu.backend.infra.ai.llm;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@Component
@ConditionalOnMissingBean(LlmClient.class)
public class StubLlmClient implements LlmClient {

    @Override
    public String generate(String prompt, String context) {
        return "";
    }
}
