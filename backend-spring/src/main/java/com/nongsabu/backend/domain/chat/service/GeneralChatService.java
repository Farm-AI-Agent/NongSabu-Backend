package com.nongsabu.backend.domain.chat.service;

import com.nongsabu.backend.domain.chat.dto.ChatRequest;
import com.nongsabu.backend.domain.chat.dto.ChatResponse;
import com.nongsabu.backend.infra.ai.tool.AgriToolService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeneralChatService {

    private static final String SYSTEM_PROMPT = """
            당신은 농업 전문 AI 비서입니다.
            농사로 농업용어사전과 NCPMS 병해충 정보 도구를 적극 활용하여
            농업 관련 질문에 실용적이고 구체적으로 한국어로 답변하세요.
            도구에서 가져온 실제 데이터를 기반으로 답변하되, 데이터가 없는 경우 일반 농업 지식을 활용하세요.
            """;

    private final ChatClient chatClient;
    private final AgriToolService agriToolService;

    public GeneralChatService(ChatClient.Builder chatClientBuilder, AgriToolService agriToolService) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.agriToolService = agriToolService;
    }

    public ChatResponse ask(Long memberId, ChatRequest request) {
        String answer = chatClient.prompt()
                .user(request.question())
                .tools(agriToolService)
                .call()
                .content();
        return new ChatResponse(
                request.question(),
                answer,
                "tool-calling",
                true,
                List.of()
        );
    }
}
