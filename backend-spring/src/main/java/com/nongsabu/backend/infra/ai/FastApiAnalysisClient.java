package com.nongsabu.backend.infra.ai;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.infra.ai.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class FastApiAnalysisClient {

    private final WebClient webClient;

    @Value("${app.ai-server.base-url}")
    private String baseUrl;

    public AiAnalysisResponse analyze(MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    })
                    .contentType(MediaType.parseMediaType(
                            file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType()
                    ));

            AiAnalysisResponse response = webClient.post()
                    .uri(baseUrl + "/api/v1/disease/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(AiAnalysisResponse.class)
                    .block();

            if (response == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "Invalid response from AI analysis server.");
            }

            return response;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Failed to call AI analysis server: " + exception.getMessage());
        }
    }
}
