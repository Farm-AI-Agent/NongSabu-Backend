package com.nongsabu.backend.infra.ai.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "enabled", havingValue = "true")
public class SpringAiLlmClient implements LlmClient {

    private static final String SYSTEM_PROMPT = """
            당신은 초보·소규모 농가를 위한 농업 AI 비서입니다.
            이미지 병충해 분석 결과, RAG 문서 근거, 외부 시세 정보를 종합해 실행 가능한 대처 리포트를 한국어로 작성하세요.
            과도한 확정 표현을 피하고, 실제 방제 전에는 지역 농업기술센터나 전문가 상담을 권고하세요.
            """;

    private final ChatClient chatClient;

    public SpringAiLlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public String generate(String prompt, String context) {
        return chatClient.prompt()
                .user("""
                        요청:
                        %s

                        참고 컨텍스트:
                        %s
                        """.formatted(prompt, context))
                .call()
                .content();
    }
}
