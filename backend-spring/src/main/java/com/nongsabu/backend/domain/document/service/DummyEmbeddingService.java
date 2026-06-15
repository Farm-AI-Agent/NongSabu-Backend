package com.nongsabu.backend.domain.document.service;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DummyEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 8;

    @Override
    public String embed(String text) {
        double[] vector = new double[DIMENSION];
        if (text == null || text.isBlank()) {
            return toVectorString(vector);
        }

        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            vector[i % DIMENSION] += chars[i];
        }

        double magnitude = 0.0;
        for (double value : vector) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0.0) {
            magnitude = 1.0;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / magnitude;
        }
        return toVectorString(vector);
    }

    private String toVectorString(double[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%.6f", vector[i]));
        }
        builder.append(']');
        return builder.toString();
    }
}

