package com.nongsabu.backend.infra.external;

import com.nongsabu.backend.domain.agri.dto.FarmDicResponse;
import java.io.StringReader;
import java.net.URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Slf4j
@Component
public class NongsaroClient {

    private final WebClient webClient;
    private final String apiKey;

    public NongsaroClient(
            WebClient webClient,
            @Value("${app.external.nongsaro-api-key:replace-me}") String apiKey
    ) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !"replace-me".equals(apiKey);
    }

    // 1단계: searchEqualWord로 wordNo 목록 조회, 2단계: detailWord로 정의 조회
    public String searchFarmDic(String word) {
        if (!isConfigured()) {
            return "농사로 API 키가 설정되지 않았습니다. NONGSARO_API_KEY 환경변수를 설정하세요.";
        }
        try {
            String searchUrl = UriComponentsBuilder.fromUri(URI.create("http://api.nongsaro.go.kr/service/farmDic/searchEqualWord"))
                    .queryParam("apiKey", apiKey)
                    .queryParam("word", word)
                    .build().encode().toUriString();

            String searchBody = webClient.get().uri(URI.create(searchUrl))
                    .retrieve().bodyToMono(String.class).block();

            if (searchBody == null || searchBody.isBlank()) {
                return word + "에 대한 농사로 사전 정보가 없습니다 (빈 응답).";
            }

            String wordNo = extractFirstWordNo(searchBody);
            if (wordNo == null) {
                return word + "에 대한 농사로 사전 일치 항목이 없습니다.";
            }

            String detailUrl = UriComponentsBuilder.fromUri(URI.create("http://api.nongsaro.go.kr/service/farmDic/detailWord"))
                    .queryParam("apiKey", apiKey)
                    .queryParam("wordNo", wordNo)
                    .build().encode().toUriString();

            String detailBody = webClient.get().uri(URI.create(detailUrl))
                    .retrieve().bodyToMono(String.class).block();

            return parseDetailWord(detailBody, word);

        } catch (Exception e) {
            log.warn("농사로 API 호출 실패: {}", e.getMessage());
            return word + " 농사로 정보를 현재 가져올 수 없습니다. (" + e.getMessage() + ")";
        }
    }

    // 구조화된 데이터 반환 (프론트엔드용)
    public FarmDicResponse searchFarmDicStructured(String word) {
        if (!isConfigured()) return FarmDicResponse.notFound(word);
        try {
            String searchUrl = UriComponentsBuilder.fromUri(URI.create("http://api.nongsaro.go.kr/service/farmDic/searchEqualWord"))
                    .queryParam("apiKey", apiKey)
                    .queryParam("word", word)
                    .build().encode().toUriString();
            String searchBody = webClient.get().uri(URI.create(searchUrl))
                    .retrieve().bodyToMono(String.class).block();
            if (searchBody == null || searchBody.isBlank()) return FarmDicResponse.notFound(word);

            String wordNo = extractFirstWordNo(searchBody);
            if (wordNo == null) return FarmDicResponse.notFound(word);

            String detailUrl = UriComponentsBuilder.fromUri(URI.create("http://api.nongsaro.go.kr/service/farmDic/detailWord"))
                    .queryParam("apiKey", apiKey)
                    .queryParam("wordNo", wordNo)
                    .build().encode().toUriString();
            String detailBody = webClient.get().uri(URI.create(detailUrl))
                    .retrieve().bodyToMono(String.class).block();
            if (detailBody == null || detailBody.isBlank()) return FarmDicResponse.notFound(word);

            Document doc = parse(detailBody);
            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) return FarmDicResponse.notFound(word);
            String wordDc = getText((Element) items.item(0), "wordDc");
            if (wordDc.isBlank()) return FarmDicResponse.notFound(word);
            String definition = wordDc.length() > 800 ? wordDc.substring(0, 800) + "..." : wordDc;
            return new FarmDicResponse(word, wordNo, definition);
        } catch (Exception e) {
            log.warn("농사로 구조화 조회 실패: {}", e.getMessage());
            return FarmDicResponse.notFound(word);
        }
    }

    private String extractFirstWordNo(String xml) throws Exception {
        Document doc = parse(xml);
        NodeList items = doc.getElementsByTagName("item");
        if (items.getLength() == 0) return null;
        Element first = (Element) items.item(0);
        String wordNo = getText(first, "wordNo");
        return wordNo.isBlank() ? null : wordNo;
    }

    private String parseDetailWord(String xml, String word) throws Exception {
        if (xml == null || xml.isBlank()) return word + "의 정의를 찾을 수 없습니다.";
        Document doc = parse(xml);
        NodeList items = doc.getElementsByTagName("item");
        if (items.getLength() == 0) return word + "의 정의를 찾을 수 없습니다.";

        Element item = (Element) items.item(0);
        String wordDc = getText(item, "wordDc");
        if (wordDc.isBlank()) return word + "의 정의를 찾을 수 없습니다.";

        String preview = wordDc.length() > 400 ? wordDc.substring(0, 400) + "..." : wordDc;
        return "[농사로 농업용어사전]\n■ " + word + "\n" + preview;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String getText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : "";
    }
}
