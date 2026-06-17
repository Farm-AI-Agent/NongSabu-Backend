package com.nongsabu.backend.domain.chat.service;

import com.nongsabu.backend.domain.chat.dto.ChatRequest;
import com.nongsabu.backend.domain.chat.dto.ChatResponse;
import com.nongsabu.backend.infra.ai.tool.AgriToolService;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeneralChatService {

    private static final String SYSTEM_PROMPT = """
            당신은 농업 전문 AI 비서입니다.
            다음 도구들을 적극 활용하여 농업 관련 질문에 실용적이고 구체적으로 한국어로 답변하세요.
            - getMarketPrice: KAMIS 농산물 가격 및 시세 정보
            - searchFarmDictionary: 농사로 농업용어사전 (작물 정보, 재배 방법, 용어 설명)
            - searchDiseasePestInfo: NCPMS 병해충 정보 (증상, 방제 방법)
            - searchGovService: 정부24 지원 서비스 (농업인 보조금, 정부 혜택)
            - searchYoungFarmerPolicy: 청년농업인 지원사업 (영농정착, 교육, 귀농 정책)
            도구에서 가져온 실제 데이터를 우선 활용하고, 데이터가 없는 경우 일반 농업 지식을 활용하세요.
            """;

    private final ChatClient chatClient;
    private final AgriToolService agriToolService;

    public GeneralChatService(ChatClient.Builder chatClientBuilder, AgriToolService agriToolService) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.agriToolService = agriToolService;
    }

    public ChatResponse ask(Long memberId, ChatRequest request) {
        try {
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
        } catch (RuntimeException exception) {
            log.warn("General chat tool-calling failed. Falling back to direct tool response.", exception);
            return new ChatResponse(
                    request.question(),
                    fallbackAnswer(request.question()),
                    "tool-fallback",
                    false,
                    List.of("direct-tool-fallback")
            );
        }
    }

    private String fallbackAnswer(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (normalized.contains("가격") || normalized.contains("시세")) {
            return agriToolService.getMarketPrice(inferCropName(question));
        }
        if (normalized.contains("병") || normalized.contains("해충") || normalized.contains("방제") || normalized.contains("증상")) {
            return agriToolService.searchDiseasePestInfo(question);
        }
        if (normalized.contains("정부") || normalized.contains("보조금") || normalized.contains("지원")) {
            return agriToolService.searchGovService(1);
        }
        if (normalized.contains("청년") || normalized.contains("영농정착") || normalized.contains("귀농")) {
            return agriToolService.searchYoungFarmerPolicy(question);
        }
        return agriToolService.searchFarmDictionary(question);
    }

    private String inferCropName(String question) {
        if (question == null || question.isBlank()) {
            return "농산물";
        }
        for (String crop : List.of("사과", "배추", "포도", "고추", "딸기", "감자", "토마토", "오이", "양파", "마늘", "복숭아", "참외", "수박", "배", "무", "상추")) {
            if (question.contains(crop)) {
                return crop;
            }
        }
        return question;
    }
}
