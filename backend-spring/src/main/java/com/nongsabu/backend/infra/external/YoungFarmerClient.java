package com.nongsabu.backend.infra.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.agri.dto.YoungFarmerResponse;
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
public class YoungFarmerClient {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public YoungFarmerClient(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${app.external.young-farmer-service-key:replace-me}") String apiKey
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !"replace-me".equals(apiKey);
    }

    // 구조화된 데이터 반환 (프론트엔드용)
    // youngV2/policyListV2: {"policy_paging":{...},"policy_list":[{...}]}
    public YoungFarmerResponse searchPoliciesStructured(String keyword, int page, int rowCnt) {
        if (!isConfigured()) {
            return new YoungFarmerResponse(page, 0, 0, List.of());
        }
        try {
            String url = buildUrl(keyword, page, rowCnt);
            String body = webClient.get().uri(URI.create(url))
                    .retrieve().bodyToMono(String.class).block();
            if (body == null || body.isBlank()) {
                return new YoungFarmerResponse(page, 0, 0, List.of());
            }
            return parseStructured(body, page);
        } catch (Exception e) {
            log.warn("청년농업인 API 호출 실패: {}", e.getMessage());
            return new YoungFarmerResponse(page, 0, 0, List.of());
        }
    }

    // LLM Tool Calling용 문자열 반환
    public String searchPoliciesForLlm(String keyword, int page, int rowCnt) {
        YoungFarmerResponse result = searchPoliciesStructured(keyword, page, rowCnt);
        if (!isConfigured()) {
            return "YOUNG_FARMER_API_KEY가 설정되지 않았습니다.";
        }
        if (result.items().isEmpty()) {
            return keyword != null
                    ? keyword + "에 대한 청년농업인 지원 정책을 찾을 수 없습니다."
                    : "청년농업인 지원 정책을 가져올 수 없습니다.";
        }
        StringBuilder sb = new StringBuilder("[청년농업인 지원사업 - 총 ")
                .append(result.totalCount()).append("건]\n");
        for (YoungFarmerResponse.Item item : result.items()) {
            sb.append("■ ").append(item.title()).append("\n");
            if (item.summary() != null && !item.summary().isBlank()) {
                String summary = item.summary().length() > 120
                        ? item.summary().substring(0, 120) + "..."
                        : item.summary();
                sb.append("  ").append(summary).append("\n");
            }
            if (item.applStDt() != null && !item.applStDt().isBlank()) {
                sb.append("  신청기간: ").append(item.applStDt())
                        .append(" ~ ").append(item.applEdDt()).append("\n");
            }
            if (item.chargeAgency() != null && !item.chargeAgency().isBlank()) {
                sb.append("  담당기관: ").append(item.chargeAgency()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String buildUrl(String keyword, int page, int rowCnt) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUri(URI.create("https://apis.data.go.kr/1390000/youngV2/policyListV2"))
                .queryParam("serviceKey", apiKey)
                .queryParam("typeDv", "json")
                .queryParam("cp", page)
                .queryParam("rowCnt", rowCnt);
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("search_keyword", keyword);
        }
        return builder.build().encode().toUriString();
    }

    private YoungFarmerResponse parseStructured(String json, int page) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode paging = root.path("policy_paging");
        int totalCount = paging.path("totalCount").asInt(0);
        int lastPage = paging.path("lastPage").asInt(0);

        JsonNode list = root.path("policy_list");
        List<YoungFarmerResponse.Item> items = new ArrayList<>();
        if (list.isArray()) {
            for (JsonNode node : list) {
                String contents = node.path("contents").asText("");
                String summary = contents.length() > 200 ? contents.substring(0, 200) + "..." : contents;
                items.add(new YoungFarmerResponse.Item(
                        node.path("seq").asText(""),
                        node.path("title").asText(""),
                        summary,
                        node.path("applStDt").asText(""),
                        node.path("applEdDt").asText(""),
                        node.path("area1Nm").asText(""),
                        node.path("chargeAgency").asText(""),
                        node.path("chargeTel").asText(""),
                        node.path("infoUrl").asText("")
                ));
            }
        }
        return new YoungFarmerResponse(page, totalCount, lastPage, items);
    }
}
