package com.nongsabu.backend.domain.prompt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nongsabu.backend.domain.document.service.DocumentService;
import com.nongsabu.backend.domain.prompt.dto.PromptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptProcessingService {

    private final DocumentService documentService;
    private final PromptProgressBroker progressBroker;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.external.openai-api-key}")
    private String openaiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public void processPrompt(String sessionId, String question) {
        new Thread(() -> {
            try {
                progressBroker.publish(sessionId, "searching", "문서에서 관련 정보를 검색 중입니다...");

                List<String> ragContextList = documentService.getContextSnippets(question, 5);
                String ragContext = String.join("\n---\n", ragContextList);

                progressBroker.publish(sessionId, "generating", "AI 응답을 생성 중입니다...");

                String systemPrompt = buildSystemPrompt(ragContext);
                String response = callOpenAI(systemPrompt, question);

                parseAndSaveResponse(sessionId, response, ragContextList);
                progressBroker.complete(sessionId);

            } catch (Exception e) {
                log.error("프롬프트 처리 중 오류 발생", e);
                progressBroker.publish(sessionId, "error", "오류 발생: " + e.getMessage());
            }
        }).start();
    }

    private String callOpenAI(String systemPrompt, String question) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", "json_object");
            requestBody.set("response_format", responseFormat);

            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.add(userMessage);

            requestBody.set("messages", messages);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String responseStr = restTemplate.postForObject(OPENAI_API_URL, entity, String.class);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            return responseNode.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류", e);
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage());
        }
    }

    private String buildSystemPrompt(String ragContext) {
        return """
                당신은 농업 전문가입니다. 사용자의 질문에 대해 정확하고 도움이 되는 답변을 제공합니다.

                다음 참고 자료를 활용하여 답변하세요:
                %s

                응답은 반드시 다음 JSON 형식으로 작성하세요:
                {
                    "answer": "질문에 대한 상세한 답변",
                    "references": ["참고자료1", "참고자료2"],
                    "confidence": "high|medium|low"
                }
                """.formatted(ragContext);
    }

    private void parseAndSaveResponse(String sessionId, String fullResponse, List<String> references) {
        try {
            JsonNode jsonNode = objectMapper.readTree(fullResponse);

            String answer = jsonNode.get("answer").asText();
            String confidence = jsonNode.get("confidence").asText();
            List<String> refList = new ArrayList<>(references);

            PromptResponse response = new PromptResponse(sessionId, answer, refList, confidence);

            progressBroker.publish(sessionId, "parsed", "응답 파싱 완료");
            log.info("프롬프트 처리 완료 - Session: {}, Confidence: {}", sessionId, confidence);

        } catch (Exception e) {
            log.error("응답 파싱 중 오류", e);
        }
    }
}
