package com.nongsabu.backend.infra.ai;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.infra.ai.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FastApiAnalysisClient {

    private final RestTemplate restTemplate;

    @Value("${app.ai-server.base-url}")
    private String baseUrl;

    public AiAnalysisResponse analyze(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(
                    file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType()
            ));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new HttpEntity<>(fileResource, fileHeaders));

            ResponseEntity<AiAnalysisResponse> response = restTemplate.exchange(
                    baseUrl + "/api/v1/analyze",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    AiAnalysisResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "Invalid response from AI analysis server.");
            }

            return response.getBody();
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Failed to call AI analysis server: " + exception.getMessage());
        }
    }
}
