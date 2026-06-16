package com.nongsabu.backend.domain.document.dto;

import java.util.List;

public record OpenSearchReindexResponse(
        int indexedCount,
        int failedCount,
        List<ReindexError> errors
) {

    public record ReindexError(
            String chunkId,
            String message
    ) {
    }
}
