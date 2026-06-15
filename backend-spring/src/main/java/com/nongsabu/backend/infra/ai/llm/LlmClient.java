package com.nongsabu.backend.infra.ai.llm;

public interface LlmClient {

    String generate(String prompt, String context);
}

