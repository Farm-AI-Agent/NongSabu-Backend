package com.nongsabu.backend.infra.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.agri.dto.Gov24Response;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public Gov24Response searchServicesStructured(int page, int perPage) {
        if (!isConfigured()) {
            return new Gov24Response(page, perPage, 0, List.of());
        }
        try {
            String url = buildUrl(page, perPage);
            byte[] responseBytes = webClient.get().uri(URI.create(url))
                    .retrieve().bodyToMono(byte[].class).block();
            String body = responseBytes == null ? null : new String(responseBytes, StandardCharsets.UTF_8);
            if (body == null || body.isBlank()) {
                return new Gov24Response(page, perPage, 0, List.of());
            }
            return parseStructured(body, page, perPage);
        } catch (Exception e) {
            log.warn("GOV24 API call failed: {}", e.getMessage());
            return new Gov24Response(page, perPage, 0, List.of());
        }
    }

    public String searchServicesForLlm(int page, int perPage) {
        Gov24Response result = searchServicesStructured(page, perPage);
        if (!isConfigured()) {
            return "GOV_SERVICE_API_KEY is not configured.";
        }
        if (result.items().isEmpty()) {
            return "No GOV24 service data is currently available.";
        }
        StringBuilder sb = new StringBuilder("[GOV24 support services - total ")
                .append(result.totalCount()).append("]\n");
        for (Gov24Response.Item item : result.items()) {
            sb.append("- ").append(item.serviceName());
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
                sb.append("  deadline: ").append(item.applyDeadline()).append("\n");
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
                        text(node, "serviceId", "\uC11C\uBE44\uC2A4ID", "\uC11C\uBE44\uC2A4 ID"),
                        text(node, "serviceName", "\uC11C\uBE44\uC2A4\uBA85"),
                        text(node, "servicePurpose", "\uC11C\uBE44\uC2A4\uBAA9\uC801\uC694\uC57D", "\uC9C0\uC6D0\uB0B4\uC6A9"),
                        text(node, "targetGroup", "\uC9C0\uC6D0\uB300\uC0C1", "\uC120\uC815\uAE30\uC900"),
                        text(node, "serviceField", "\uC11C\uBE44\uC2A4\uBD84\uC57C"),
                        text(node, "applyDeadline", "\uC2E0\uCCAD\uAE30\uD55C"),
                        text(node, "applyMethod", "\uC2E0\uCCAD\uBC29\uBC95"),
                        text(node, "department", "\uC18C\uAD00\uAE30\uAD00\uBA85", "\uC811\uC218\uAE30\uAD00\uBA85"),
                        text(node, "detailUrl", "\uC0C1\uC138\uC870\uD68CURL", "\uC628\uB77C\uC778\uC2E0\uCCAD\uC0AC\uC774\uD2B8URL")
                ));
            }
        }
        return new Gov24Response(page, perPage, totalCount, items);
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            String value = node.path(name).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }

        List<String> normalizedNames = new ArrayList<>();
        for (String name : names) {
            normalizedNames.add(normalize(name));
        }

        var fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (normalizedNames.contains(normalize(field.getKey()))) {
                String value = field.getValue().asText("");
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("[\\s_\\-]", "").toLowerCase();
    }
}
