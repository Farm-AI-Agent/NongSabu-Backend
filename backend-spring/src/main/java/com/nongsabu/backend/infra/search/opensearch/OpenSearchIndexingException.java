package com.nongsabu.backend.infra.search.opensearch;

public class OpenSearchIndexingException extends RuntimeException {

    public OpenSearchIndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
