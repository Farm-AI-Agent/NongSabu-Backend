package com.nongsabu.backend.infra.ai.llm;

import org.springframework.stereotype.Component;

@Component
public class StubLlmClient implements LlmClient {

    @Override
    public String generate(String prompt, String context) {
        return "TODO: Replace with stable Spring AI or provider-specific LLM integration.";
    }
}
