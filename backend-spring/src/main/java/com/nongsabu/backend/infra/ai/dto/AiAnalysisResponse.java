package com.nongsabu.backend.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AiAnalysisResponse(
        boolean success,
        String diagnosis,
        double confidence,
        String severity,
        String summary,
        @JsonProperty("recommended_action")
        String recommendedAction,
        @JsonProperty("model_version")
        String modelVersion,
        List<Detection> detections,
        @JsonProperty("detection_count")
        Integer detectionCount,
        @JsonProperty("image_size")
        Map<String, Integer> imageSize
) {

    public AiAnalysisResponse(
            boolean success,
            String diagnosis,
            double confidence,
            String severity,
            String summary,
            String recommendedAction,
            String modelVersion
    ) {
        this(success, diagnosis, confidence, severity, summary, recommendedAction, modelVersion, List.of(), 0, Map.of());
    }

    public List<Detection> safeDetections() {
        return detections == null ? List.of() : detections;
    }

    public int safeDetectionCount() {
        return detectionCount == null ? safeDetections().size() : detectionCount;
    }

    public Map<String, Integer> safeImageSize() {
        return imageSize == null ? Map.of() : imageSize;
    }

    public record Detection(
            @JsonProperty("class_name")
            String className,
            String label,
            double confidence,
            @JsonProperty("confidence_percent")
            Double confidencePercent,
            List<Double> bbox
    ) {

        public Detection(String className, String label, double confidence, List<Double> bbox) {
            this(className, label, confidence, roundConfidencePercent(confidence), bbox);
        }

        private static double roundConfidencePercent(double confidence) {
            return Math.round(confidence * 1000.0) / 10.0;
        }
    }
}
