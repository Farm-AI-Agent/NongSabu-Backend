package com.nongsabu.backend.domain.debug.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.debug.dto.DebugIntegrationStatusResponse;
import com.nongsabu.backend.domain.debug.dto.DebugProbeResponse;
import com.nongsabu.backend.domain.debug.dto.DebugToolInvokeRequest;
import com.nongsabu.backend.domain.debug.dto.DebugToolInvokeResponse;
import com.nongsabu.backend.domain.document.service.RagService;
import com.nongsabu.backend.domain.externalapilog.service.ExternalApiLogService;
import com.nongsabu.backend.infra.ai.llm.LlmClient;
import com.nongsabu.backend.infra.external.KamisClient;
import com.nongsabu.backend.infra.mcp.McpToolClient;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class DebugIntegrationService {

    private static final int DEFAULT_TOP_K = 4;

    private final Environment environment;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RagService ragService;
    private final KamisClient kamisClient;
    private final ObjectProvider<LlmClient> llmClientProvider;
    private final McpToolClient mcpToolClient;
    private final ExternalApiLogService externalApiLogService;

    public DebugIntegrationStatusResponse status() {
        return new DebugIntegrationStatusResponse(
                List.of(
                        keyStatus("OpenAI", "OPENAI_API_KEY", "spring.ai.openai.api-key", "RAG embedding/chat model key"),
                        keyStatus("KAMIS", "KAMIS_API_KEY", "app.external.kamis-api-key", "Market price key. Client is still a stub."),
                        keyStatus("KAMIS customer", "KAMIS_CUSTOMER_ID", "app.external.kamis-customer-id", "KAMIS customer id"),
                        keyStatus("NCPMS", "NCPMS_API_KEY", "app.external.ncpms-api-key", "Needed for disease/pest lookup tool"),
                        keyStatus("Nongsaro", "NONGSARO_API_KEY", "app.external.nongsaro-api-key", "Needed for agri dictionary lookup tool"),
                        keyStatus("Government service", "GOV_SERVICE_API_KEY", "app.external.gov-service-key", "Needed for support program collection"),
                        keyStatus("Young farmer", "YOUNG_FARMER_API_KEY", "app.external.young-farmer-service-key", "Needed for youth farmer policy collection")
                ),
                List.of(
                        endpoint("FastAPI disease server", "app.ai-server.base-url", "/health"),
                        endpoint("OpenSearch", "app.opensearch.base-url", ""),
                        endpoint("Reranker", "app.reranker.base-url", "/health"),
                        new DebugIntegrationStatusResponse.EndpointStatus(
                                "KAMIS smoke",
                                "KAMIS periodRetailProductList",
                                "Use /api/v1/debug/integrations/probe/kamis."
                        ),
                        new DebugIntegrationStatusResponse.EndpointStatus(
                                "NCPMS smoke",
                                "NCPMS SVC05 sick detail",
                                "Use /api/v1/debug/integrations/probe/ncpms."
                        ),
                        new DebugIntegrationStatusResponse.EndpointStatus(
                                "Nongsaro smoke",
                                "Nongsaro farmDic/searchEqualWord",
                                "Use /api/v1/debug/integrations/probe/nongsaro."
                        ),
                        new DebugIntegrationStatusResponse.EndpointStatus(
                                "Gov service smoke",
                                "ODCloud gov24 serviceList",
                                "Use /api/v1/debug/integrations/probe/gov-service."
                        ),
                        new DebugIntegrationStatusResponse.EndpointStatus(
                                "Young farmer smoke",
                                "data.go.kr youngV2 policyListV2",
                                "Use /api/v1/debug/integrations/probe/young-farmer."
                        )
                ),
                List.of(
                        "rag_search",
                        "rag_ask",
                        "kamis_market_snapshot",
                        "llm_generate",
                        "mcp_stub"
                )
        );
    }

    public DebugProbeResponse probe(String target) {
        String normalized = normalize(target);
        return switch (normalized) {
            case "fastapi", "ai", "ai_server" -> internalProbe(normalized, baseUrl("app.ai-server.base-url") + "/health");
            case "opensearch" -> internalProbe(normalized, baseUrl("app.opensearch.base-url"));
            case "reranker" -> internalProbe(normalized, baseUrl("app.reranker.base-url") + "/health");
            case "kamis" -> kamisProbe();
            case "ncpms" -> ncpmsProbe();
            case "nongsaro" -> nongsaroProbe();
            case "gov_service", "govservice", "gov" -> govServiceProbe();
            case "young_farmer", "youngfarmer" -> youngFarmerProbe();
            default -> throw new IllegalArgumentException("Unknown probe target: " + target);
        };
    }

    private DebugProbeResponse internalProbe(String target, String url) {
        long started = System.nanoTime();
        try {
            ProbePayload payload = webClient.get()
                    .uri(url)
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new ProbePayload(response.statusCode(), body)))
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (payload == null) {
                return probeResponse(target, true, true, false, null, elapsed(started), url, Map.of(), "", "Empty probe response");
            }
            return probeResponse(
                    target,
                    true,
                    true,
                    payload.statusCode().is2xxSuccessful(),
                    payload.statusCode().value(),
                    elapsed(started),
                    url,
                    Map.of("type", "internal-service-health"),
                    preview(payload.body()),
                    payload.statusCode().is2xxSuccessful() ? null : "Non-2xx response"
            );
        } catch (RuntimeException exception) {
            return probeResponse(target, true, true, false, null, elapsed(started), url, Map.of(), "", exception.getMessage());
        }
    }

    private DebugProbeResponse kamisProbe() {
        String apiKey = value("KAMIS_API_KEY", "app.external.kamis-api-key");
        String customerId = value("KAMIS_CUSTOMER_ID", "app.external.kamis-customer-id");
        Map<String, Object> summary = Map.of(
                "action", "periodRetailProductList",
                "returnType", "json",
                "uses", "p_cert_key + p_cert_id"
        );
        if (!isConfigured(apiKey)) {
            return externalFailFast("kamis", "KAMIS", "KAMIS periodRetailProductList", summary,
                    "KAMIS_API_KEY is required as p_cert_key");
        }
        if (!isConfigured(customerId)) {
            return externalFailFast("kamis", "KAMIS", "KAMIS periodRetailProductList", summary,
                    "KAMIS_CUSTOMER_ID is required as p_cert_id");
        }

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
                .build()
                .encode()
                .toUriString();
        return externalGet("kamis", "KAMIS", "KAMIS periodRetailProductList", url, summary);
    }

    private DebugProbeResponse ncpmsProbe() {
        String apiKey = value("NCPMS_API_KEY", "app.external.ncpms-api-key");
        Map<String, Object> summary = Map.of(
                "serviceCode", "SVC05",
                "sickKey", "D00000030"
        );
        if (!isConfigured(apiKey)) {
            return externalFailFast("ncpms", "NCPMS", "NCPMS SVC05 sick detail", summary,
                    "NCPMS_API_KEY is required as apiKey");
        }
        String url = UriComponentsBuilder.fromUri(URI.create("http://ncpms.rda.go.kr/npmsAPI/service"))
                .queryParam("apiKey", apiKey)
                .queryParam("serviceCode", "SVC05")
                .queryParam("sickKey", "D00000030")
                .build()
                .encode()
                .toUriString();
        return externalGet("ncpms", "NCPMS", "NCPMS SVC05 sick detail", url, summary);
    }

    private DebugProbeResponse nongsaroProbe() {
        String apiKey = value("NONGSARO_API_KEY", "app.external.nongsaro-api-key");
        Map<String, Object> summary = Map.of(
                "serviceName", "farmDic",
                "operationName", "searchEqualWord",
                "word", "포도"
        );
        if (!isConfigured(apiKey)) {
            return externalFailFast("nongsaro", "NONGSARO", "Nongsaro farmDic/searchEqualWord", summary,
                    "NONGSARO_API_KEY is required as apiKey");
        }
        String url = UriComponentsBuilder.fromUri(URI.create("http://api.nongsaro.go.kr/service/farmDic/searchEqualWord"))
                .queryParam("apiKey", apiKey)
                .queryParam("word", "포도")
                .build()
                .encode()
                .toUriString();
        return externalGet("nongsaro", "NONGSARO", "Nongsaro farmDic/searchEqualWord", url, summary);
    }

    private DebugProbeResponse govServiceProbe() {
        String apiKey = value("GOV_SERVICE_API_KEY", "app.external.gov-service-key");
        Map<String, Object> summary = Map.of(
                "dataset", "gov24",
                "operation", "serviceList",
                "page", 1,
                "perPage", 1
        );
        if (!isConfigured(apiKey)) {
            return externalFailFast("gov_service", "GOV_SERVICE", "ODCloud gov24 serviceList", summary,
                    "GOV_SERVICE_API_KEY is required as serviceKey");
        }
        String url = UriComponentsBuilder.fromUri(URI.create("https://api.odcloud.kr/api/gov24/v3/serviceList"))
                .queryParam("page", 1)
                .queryParam("perPage", 1)
                .queryParam("serviceKey", apiKey)
                .build()
                .encode()
                .toUriString();
        return externalGet("gov_service", "GOV_SERVICE", "ODCloud gov24 serviceList", url, summary);
    }

    private DebugProbeResponse youngFarmerProbe() {
        String apiKey = value("YOUNG_FARMER_API_KEY", "app.external.young-farmer-service-key");
        Map<String, Object> summary = Map.of(
                "endpoint", "youngV2",
                "operation", "policyListV2",
                "authParam", "ServiceKey",
                "cp", 1,
                "rowCnt", 1,
                "typeDv", "json"
        );
        if (!isConfigured(apiKey)) {
            return externalFailFast("young_farmer", "YOUNG_FARMER", "data.go.kr youngV2 policyListV2", summary,
                    "YOUNG_FARMER_API_KEY is required as ServiceKey");
        }
        String url = UriComponentsBuilder.fromUri(URI.create("https://apis.data.go.kr/1390000/youngV2/policyListV2"))
                .queryParam("serviceKey", apiKey)
                .queryParam("typeDv", "json")
                .queryParam("cp", 1)
                .queryParam("rowCnt", 1)
                .build()
                .encode()
                .toUriString();
        return externalGet("young_farmer", "YOUNG_FARMER", "data.go.kr youngV2 policyListV2", url, summary);
    }

    private DebugProbeResponse externalFailFast(
            String target,
            String provider,
            String endpoint,
            Map<String, Object> requestSummary,
            String errorMessage
    ) {
        DebugProbeResponse response = probeResponse(target, false, false, false, null, 0L, endpoint, requestSummary, "", errorMessage);
        externalApiLogService.logDebugProbe(provider, endpoint, requestSummary, "", null, false, errorMessage, 0L);
        return response;
    }

    private DebugProbeResponse externalGet(
            String target,
            String provider,
            String endpoint,
            String url,
            Map<String, Object> requestSummary
    ) {
        long started = System.nanoTime();
        try {
            ProbePayload payload = webClient.get()
                    .uri(url)
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new ProbePayload(response.statusCode(), body)))
                    .timeout(Duration.ofSeconds(12))
                    .block();
            long elapsed = elapsed(started);
            if (payload == null) {
                externalApiLogService.logDebugProbe(provider, endpoint, requestSummary, "", null, false, "Empty probe response", elapsed);
                return probeResponse(target, true, true, false, null, elapsed, endpoint, requestSummary, "", "Empty probe response");
            }
            boolean success = payload.statusCode().is2xxSuccessful();
            String preview = preview(payload.body());
            String errorMessage = success ? null : "Non-2xx response";
            externalApiLogService.logDebugProbe(provider, endpoint, requestSummary, preview, payload.statusCode().value(), success, errorMessage, elapsed);
            return probeResponse(target, true, true, success, payload.statusCode().value(), elapsed, endpoint, requestSummary, preview, errorMessage);
        } catch (RuntimeException exception) {
            long elapsed = elapsed(started);
            String errorMessage = safeErrorMessage(exception);
            externalApiLogService.logDebugProbe(provider, endpoint, requestSummary, "", null, false, errorMessage, elapsed);
            return probeResponse(target, true, true, false, null, elapsed, endpoint, requestSummary, "", errorMessage);
        }
    }

    private DebugProbeResponse probeResponse(
            String target,
            boolean configured,
            boolean attempted,
            boolean success,
            Integer statusCode,
            long elapsedMillis,
            String endpoint,
            Map<String, Object> requestSummary,
            String responsePreview,
            String errorMessage
    ) {
        return new DebugProbeResponse(
                target,
                configured,
                attempted,
                success,
                statusCode,
                elapsedMillis,
                endpoint,
                requestSummary,
                responsePreview,
                errorMessage
        );
    }

    public DebugToolInvokeResponse invoke(Long memberId, DebugToolInvokeRequest request) {
        String toolName = normalize(request.toolName());
        long started = System.nanoTime();
        try {
            Object result = switch (toolName) {
                case "rag_search" -> ragService.search(
                        memberId,
                        required(firstNonBlank(request.query(), stringPayload(request, "query")), "query"),
                        topK(request),
                        firstNonBlank(request.retrievalMode(), stringPayload(request, "retrievalMode"))
                );
                case "rag_ask" -> ragService.ask(
                        memberId,
                        required(firstNonBlank(request.query(), stringPayload(request, "question")), "query/question"),
                        topK(request),
                        firstNonBlank(request.retrievalMode(), stringPayload(request, "retrievalMode"))
                );
                case "kamis_market_snapshot" -> kamisClient.getMarketSnapshot(
                        required(firstNonBlank(request.cropName(), stringPayload(request, "cropName")), "cropName")
                );
                case "llm_generate" -> llmClient().generate(
                        required(firstNonBlank(request.prompt(), stringPayload(request, "prompt")), "prompt"),
                        firstNonBlank(request.context(), stringPayload(request, "context"), "")
                );
                case "mcp_stub" -> mcpToolClient.invoke(
                        firstNonBlank(stringPayload(request, "toolName"), "debug.stub"),
                        toJson(request.payload() == null ? Map.of() : request.payload())
                );
                default -> throw new IllegalArgumentException("Unknown debug tool: " + request.toolName());
            };
            return new DebugToolInvokeResponse(toolName, true, result, null, elapsed(started));
        } catch (RuntimeException exception) {
            return new DebugToolInvokeResponse(toolName, false, null, exception.getMessage(), elapsed(started));
        }
    }

    private LlmClient llmClient() {
        LlmClient client = llmClientProvider.getIfAvailable();
        if (client == null) {
            return (prompt, context) -> "LlmClient bean is not available. Check app.llm.enabled and OpenAI/Spring AI configuration.";
        }
        return client;
    }

    private DebugIntegrationStatusResponse.ApiKeyStatus keyStatus(
            String name,
            String envName,
            String propertyName,
            String note
    ) {
        String value = firstNonBlank(System.getenv(envName), propertyName == null ? null : environment.getProperty(propertyName));
        boolean configured = isConfigured(value);
        return new DebugIntegrationStatusResponse.ApiKeyStatus(
                name,
                configured,
                propertyName == null ? envName : propertyName + " / " + envName,
                mask(value),
                note
        );
    }

    private DebugIntegrationStatusResponse.EndpointStatus endpoint(String name, String propertyName, String suffix) {
        return new DebugIntegrationStatusResponse.EndpointStatus(
                name,
                baseUrl(propertyName) + suffix,
                "Use /api/v1/debug/integrations/probe/{target} to test connectivity."
        );
    }

    private String baseUrl(String propertyName) {
        String value = environment.getProperty(propertyName);
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private String value(String envName, String propertyName) {
        return firstNonBlank(System.getenv(envName), propertyName == null ? null : environment.getProperty(propertyName));
    }

    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String sanitized = message;
        for (String secret : List.of(
                value("OPENAI_API_KEY", "spring.ai.openai.api-key"),
                value("KAMIS_API_KEY", "app.external.kamis-api-key"),
                value("KAMIS_CUSTOMER_ID", "app.external.kamis-customer-id"),
                value("NCPMS_API_KEY", "app.external.ncpms-api-key"),
                value("NONGSARO_API_KEY", "app.external.nongsaro-api-key"),
                value("GOV_SERVICE_API_KEY", "app.external.gov-service-key"),
                value("YOUNG_FARMER_API_KEY", "app.external.young-farmer-service-key")
        )) {
            if (isConfigured(secret)) {
                sanitized = sanitized.replace(secret, "(redacted)");
            }
        }
        return exception.getClass().getSimpleName() + ": " + sanitized;
    }

    private int topK(DebugToolInvokeRequest request) {
        Integer topK = request.topK();
        if (topK == null && request.payload() != null && request.payload().get("topK") instanceof Number number) {
            topK = number.intValue();
        }
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(topK, 20));
    }

    private String stringPayload(DebugToolInvokeRequest request, String key) {
        if (request.payload() == null) {
            return null;
        }
        Object value = request.payload().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isConfigured(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return !normalized.equals("replace-me")
                && !normalized.equals("not-configured")
                && !normalized.startsWith("replace-this");
    }

    private String mask(String value) {
        if (!isConfigured(value)) {
            return "(not configured)";
        }
        if (value.length() <= 8) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 4) + "...(" + value.length() + " chars)";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 800 ? compact : compact.substring(0, 800);
    }

    private long elapsed(long startedNanoTime) {
        return Duration.ofNanos(System.nanoTime() - startedNanoTime).toMillis();
    }

    private record ProbePayload(HttpStatusCode statusCode, String body) {
    }

    public Map<String, Object> samplePayloads() {
        Map<String, Object> samples = new LinkedHashMap<>();
        samples.put("rag_search", Map.of(
                "toolName", "rag_search",
                "query", "포도 병해 관리 방법",
                "topK", 4,
                "retrievalMode", "hybrid"
        ));
        samples.put("rag_ask", Map.of(
                "toolName", "rag_ask",
                "query", "업로드한 문서를 바탕으로 포도 병해 관리 방법을 알려줘.",
                "topK", 4,
                "retrievalMode", "hybrid-rerank"
        ));
        samples.put("kamis_market_snapshot", Map.of(
                "toolName", "kamis_market_snapshot",
                "cropName", "포도"
        ));
        samples.put("llm_generate", Map.of(
                "toolName", "llm_generate",
                "prompt", "초보 농가에게 포도 잎 병반 대응 방법을 요약해줘.",
                "context", "테스트 컨텍스트입니다."
        ));
        samples.put("mcp_stub", Map.of(
                "toolName", "mcp_stub",
                "payload", Map.of(
                        "toolName", "debug.echo",
                        "message", "MCP stub connectivity test"
                )
        ));
        return samples;
    }
}
