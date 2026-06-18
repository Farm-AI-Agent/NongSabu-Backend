package com.nongsabu.backend.domain.document.service;

public enum RetrievalMode {
    VECTOR,
    BM25,
    HYBRID,
    HYBRID_RERANK;

    public static RetrievalMode from(String value) {
        if (value == null || value.isBlank()) {
            return VECTOR;
        }
        return RetrievalMode.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
