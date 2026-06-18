package com.nongsabu.backend.domain.image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.image.entity.AnalysisStatus;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AnalysisResponse(
        Long imageId,
        Long cropId,
        String cropName,
        String filename,
        LocalDateTime createdAt,
        String status,
        boolean supported,
        String message,
        String diseaseName,
        double confidence,
        String severity,
        String summary,
        String recommendation,
        List<DetectionResponse> detections,
        int detectionCount,
        Map<String, Integer> imageSize
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static AnalysisResponse of(UploadedImage image, ImageAnalysisResult result) {
        AnalysisStatus status = image.getAnalysisStatus();
        RawAnalysisPayload rawPayload = parseRawPayload(result);
        return new AnalysisResponse(
                image.getId(),
                image.getCrop() == null ? null : image.getCrop().getId(),
                image.getCrop() == null ? null : image.getCrop().getName(),
                image.getOriginalFilename(),
                image.getCreatedAt(),
                status.name(),
                status != AnalysisStatus.UNSUPPORTED,
                resolveMessage(status, result),
                result == null ? null : result.getDiseaseName(),
                result == null ? 0.0 : result.getConfidence(),
                status == AnalysisStatus.UNSUPPORTED || result == null ? null : result.getSeverity(),
                result == null ? null : result.getSummary(),
                result == null ? null : result.getRecommendation(),
                rawPayload.detections(),
                rawPayload.detectionCount(),
                rawPayload.imageSize()
        );
    }

    private static String resolveMessage(AnalysisStatus status, ImageAnalysisResult result) {
        if (status == AnalysisStatus.UNSUPPORTED && result != null && result.getSummary() != null) {
            return result.getSummary();
        }
        return switch (status) {
            case PENDING -> "이미지 분석 대기 중입니다.";
            case PROCESSING -> "이미지 분석을 진행 중입니다.";
            case COMPLETED -> "이미지 분석이 완료되었습니다.";
            case UNSUPPORTED -> "현재 MVP에서는 포도 병충해 분석만 지원합니다.";
            case FAILED -> "이미지 분석에 실패했습니다.";
        };
    }

    private static RawAnalysisPayload parseRawPayload(ImageAnalysisResult result) {
        if (result == null || result.getRawResponse() == null || result.getRawResponse().isBlank()) {
            return RawAnalysisPayload.empty();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(result.getRawResponse());
            List<DetectionResponse> detections = readDetections(root.path("detections"));
            int detectionCount = root.path("detection_count").isInt()
                    ? root.path("detection_count").asInt()
                    : detections.size();
            Map<String, Integer> imageSize = readImageSize(root.path("image_size"));
            return new RawAnalysisPayload(detections, detectionCount, imageSize);
        } catch (JsonProcessingException exception) {
            return RawAnalysisPayload.empty();
        }
    }

    private static List<DetectionResponse> readDetections(JsonNode detectionsNode) {
        if (!detectionsNode.isArray()) {
            return List.of();
        }
        return OBJECT_MAPPER.convertValue(
                detectionsNode,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, DetectionResponse.class)
        );
    }

    private static Map<String, Integer> readImageSize(JsonNode imageSizeNode) {
        if (!imageSizeNode.isObject()) {
            return Map.of();
        }
        Integer width = imageSizeNode.path("width").isNumber() ? imageSizeNode.path("width").asInt() : null;
        Integer height = imageSizeNode.path("height").isNumber() ? imageSizeNode.path("height").asInt() : null;
        if (width == null || height == null) {
            return Map.of();
        }
        return Map.of("width", width, "height", height);
    }

    public record DetectionResponse(
            @JsonProperty("class_name")
            String className,
            String label,
            double confidence,
            List<Double> bbox
    ) {
    }

    private record RawAnalysisPayload(
            List<DetectionResponse> detections,
            @JsonProperty("detection_count")
            Integer detectionCount,
            @JsonProperty("image_size")
            Map<String, Integer> imageSize
    ) {

        private static RawAnalysisPayload empty() {
            return new RawAnalysisPayload(List.of(), 0, Map.of());
        }

    }
}
