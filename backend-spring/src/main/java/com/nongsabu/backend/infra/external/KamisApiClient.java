package com.nongsabu.backend.infra.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class KamisApiClient implements KamisClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String customerId;

    public KamisApiClient(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${app.external.kamis-api-key:replace-me}") String apiKey,
            @Value("${app.external.kamis-customer-id:replace-me}") String customerId
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.customerId = customerId;
    }

    @Override
    public String getMarketSnapshot(String cropName) {
        if ("replace-me".equals(apiKey) || "replace-me".equals(customerId)) {
            return cropName + " 가격 정보를 가져오려면 KAMIS_API_KEY와 KAMIS_CUSTOMER_ID 환경변수를 설정해야 합니다.";
        }
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String url = UriComponentsBuilder.fromUri(URI.create("https://www.kamis.or.kr/service/price/xml.do"))
                    .queryParam("action", "periodRetailProductList")
                    .queryParam("p_product_cls_code", "01")
                    .queryParam("p_country_code", "1101")
                    .queryParam("p_regday", today)
                    .queryParam("p_convert_kg_yn", "N")
                    .queryParam("p_item_category_code", "200")
                    .queryParam("p_cert_key", apiKey)
                    .queryParam("p_cert_id", customerId)
                    .queryParam("p_returntype", "json")
                    .build().encode().toUriString();

            String body = webClient.get().uri(URI.create(url))
                    .retrieve().bodyToMono(String.class).block();

            if (body == null || body.isBlank()) {
                return cropName + " 가격 정보를 찾을 수 없습니다 (빈 응답).";
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("data").path("item");

            if (!items.isArray() || items.isEmpty()) {
                return cropName + " 가격 정보를 찾을 수 없습니다.";
            }

            StringBuilder result = new StringBuilder("[KAMIS 소매 가격 정보] ").append(today).append("\n");
            int matched = 0;
            for (JsonNode item : items) {
                String itemName = item.path("item_name").asText("");
                if (!itemName.isEmpty() && (cropName.isBlank() || itemName.contains(cropName) || cropName.contains(itemName))) {
                    String unit = item.path("unit").asText("-");
                    String price = item.path("dpr1").asText("-");
                    result.append("- ").append(itemName).append(" (").append(unit).append("): ").append(price).append("원\n");
                    if (++matched >= 5) break;
                }
            }

            if (matched == 0) {
                int shown = 0;
                for (JsonNode item : items) {
                    String itemName = item.path("item_name").asText("");
                    if (!itemName.isEmpty()) {
                        String unit = item.path("unit").asText("-");
                        String price = item.path("dpr1").asText("-");
                        result.append("- ").append(itemName).append(" (").append(unit).append("): ").append(price).append("원\n");
                        if (++shown >= 3) break;
                    }
                }
                if (shown == 0) {
                    return cropName + " 가격 정보를 찾을 수 없습니다.";
                }
            }

            return result.toString().trim();
        } catch (Exception e) {
            log.warn("KAMIS API 호출 실패: {}", e.getMessage());
            return cropName + " 가격 정보를 현재 가져올 수 없습니다. (" + e.getMessage() + ")";
        }
    }
}
