package com.nongsabu.backend.infra.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.agri.dto.DiseasePestResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class NcpmsClient {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public NcpmsClient(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${app.external.ncpms-api-key:replace-me}") String apiKey
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !"replace-me".equals(apiKey);
    }

    // SVC16 통합검색: searchName 파라미터로 병해충/작물 이름 검색 (JSON 응답)
    public String searchDiseasePest(String query) {
        if (!isConfigured()) {
            return "NCPMS API 키가 설정되지 않았습니다. NCPMS_API_KEY 환경변수를 설정하세요.";
        }
        try {
            String url = UriComponentsBuilder.fromUri(URI.create("http://ncpms.rda.go.kr/npmsAPI/service"))
                    .queryParam("apiKey", apiKey)
                    .queryParam("serviceCode", "SVC16")
                    .queryParam("searchName", query)
                    .build().encode().toUriString();

            String body = webClient.get().uri(URI.create(url))
                    .retrieve().bodyToMono(String.class).block();

            if (body == null || body.isBlank()) {
                return query + "에 대한 병해충 정보가 없습니다 (빈 응답).";
            }

            return parseJsonNcpms(body, query);
        } catch (Exception e) {
            log.warn("NCPMS API 호출 실패: {}", e.getMessage());
            return query + " 병해충 정보를 현재 가져올 수 없습니다. (" + e.getMessage() + ")";
        }
    }

    // 구조화된 데이터 반환 (프론트엔드용)
    public DiseasePestResponse searchDiseasePestStructured(String query) {
        if (!isConfigured()) return new DiseasePestResponse(query, 0, List.of());
        try {
            String url = UriComponentsBuilder.fromUri(URI.create("http://ncpms.rda.go.kr/npmsAPI/service"))
                    .queryParam("apiKey", apiKey)
                    .queryParam("serviceCode", "SVC16")
                    .queryParam("searchName", query)
                    .build().encode().toUriString();
            String body = webClient.get().uri(URI.create(url))
                    .retrieve().bodyToMono(String.class).block();
            if (body == null || body.isBlank()) return new DiseasePestResponse(query, 0, List.of());
            return parseStructured(body, query);
        } catch (Exception e) {
            log.warn("NCPMS 구조화 조회 실패: {}", e.getMessage());
            return new DiseasePestResponse(query, 0, List.of());
        }
    }

    private DiseasePestResponse parseStructured(String json, String query) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode service = root.path("service");
        if (service.isMissingNode()) return new DiseasePestResponse(query, 0, List.of());

        int totalCount = service.path("totalCount").asInt(0);
        JsonNode list = service.path("list");
        List<DiseasePestResponse.Item> items = new ArrayList<>();
        if (list.isArray()) {
            for (JsonNode item : list) {
                items.add(new DiseasePestResponse.Item(
                        item.path("korName").asText(""),
                        item.path("divName").asText(""),
                        item.path("cropName").asText(""),
                        item.path("oprName").asText(""),
                        item.path("thumbImg").asText(""),
                        item.path("detailUrl").asText("")
                ));
            }
        }
        return new DiseasePestResponse(query, totalCount, items);
    }

    // NCPMS SVC16 응답 형식: {"service":{"list":[{"korName":"...","divName":"...","cropName":"...","oprName":"..."}]}}
    private String parseJsonNcpms(String json, String query) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode service = root.path("service");

        if (service.isMissingNode()) {
            return query + "에 대한 병해충 정보가 없습니다 (응답 형식 오류).";
        }

        JsonNode list = service.path("list");
        if (list.isMissingNode() || !list.isArray() || list.size() == 0) {
            return query + "에 대한 병해충 정보가 없습니다.";
        }

        StringBuilder result = new StringBuilder("[NCPMS 병해충 정보 - ").append(query).append("]\n");
        int count = Math.min(list.size(), 5);
        for (int i = 0; i < count; i++) {
            JsonNode item = list.get(i);
            String korName = item.path("korName").asText("");
            String divName = item.path("divName").asText("");
            String cropName = item.path("cropName").asText("");
            String oprName = item.path("oprName").asText("");

            if (!korName.isBlank()) {
                result.append("■ ").append(korName);
                if (!divName.isBlank()) result.append(" [").append(divName).append("]");
                if (!cropName.isBlank()) result.append(" / 작물: ").append(cropName);
                result.append("\n");
                if (!oprName.isBlank()) result.append("  학명: ").append(oprName).append("\n");
            }
        }
        return result.toString().trim();
    }
}
