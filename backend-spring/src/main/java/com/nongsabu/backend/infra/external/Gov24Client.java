package com.nongsabu.backend.infra.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.agri.dto.Gov24Response;
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
public class Gov24Client {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public Gov24Client(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${app.external.gov-service-key:replace-me}") String apiKey
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !"replace-me".equals(apiKey);
    }

    // 구조화된 데이터 반환 (프론트엔드용)
    public Gov24Response searchServicesStructured(int page, int perPage) {
        if (!isConfigured()) {
            return new Gov24Response(page, perPage, 0, List.of());
        }
        try {
            String url = buildUrl(page, perPage);
            String body = webClient.get().uri(URI.create(url))
                    .retrieve().bodyToMono(String.class).block();
            if (body == null || body.isBlank()) {
                return new Gov24Response(page, perPage, 0, List.of());
            }
            return parseStructured(body, page, perPage);
        } catch (Exception e) {
            log.warn("GOV24 API 호출 실패: {}", e.getMessage());
            return new Gov24Response(page, perPage, 0, List.of());
        }
    }

    // LLM Tool Calling용 문자열 반환
    public String searchServicesForLlm(int page, int perPage) {
        Gov24Response result = searchServicesStructured(page, perPage);
        if (!isConfigured()) {
            return "GOV_SERVICE_API_KEY가 설정되지 않았습니다.";
        }
        if (result.items().isEmpty()) {
            return "현재 정부24 서비스 정보를 가져올 수 없습니다.";
        }
        StringBuilder sb = new StringBuilder("[정부24 지원서비스 - 총 ")
                .append(result.totalCount()).append("건]\n");
        for (Gov24Response.Item item : result.items()) {
            sb.append("■ ").append(item.serviceName());
            if (item.serviceField() != null && !item.serviceField().isBlank()) {
                sb.append(" [").append(item.serviceField()).append("]");
            }
            sb.append("\n");
            if (item.servicePurpose() != null && !item.servicePurpose().isBlank()) {
                String purpose = item.servicePurpose().length() > 100
                        ? item.servicePurpose().substring(0, 100) + "..."
                        : item.servicePurpose();
                sb.append("  ").append(purpose).append("\n");
            }
            if (item.applyDeadline() != null && !item.applyDeadline().isBlank()) {
                sb.append("  신청기한: ").append(item.applyDeadline()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String buildUrl(int page, int perPage) {
        return UriComponentsBuilder.fromUri(URI.create("https://api.odcloud.kr/api/gov24/v3/serviceList"))
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("serviceKey", apiKey)
                .build().encode().toUriString();
    }

    private Gov24Response parseStructured(String json, int page, int perPage) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        int totalCount = root.path("totalCount").asInt(0);
        JsonNode data = root.path("data");

        List<Gov24Response.Item> items = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode node : data) {
                items.add(new Gov24Response.Item(
                        node.path("서비스ID").asText(""),
                        node.path("서비스명").asText(""),
                        node.path("서비스목적요약").asText(""),
                        node.path("사용자구분").asText(""),
                        node.path("서비스분야").asText(""),
                        node.path("신청기한").asText(""),
                        node.path("신청방법").asText(""),
                        node.path("부서명").asText(""),
                        node.path("상세조회URL").asText("")
                ));
            }
        }
        return new Gov24Response(page, perPage, totalCount, items);
    }
}
