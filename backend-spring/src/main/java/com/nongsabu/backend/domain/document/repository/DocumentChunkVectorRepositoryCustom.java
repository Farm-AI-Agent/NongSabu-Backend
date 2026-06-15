package com.nongsabu.backend.domain.document.repository;

import java.util.List;
import com.nongsabu.backend.domain.document.entity.DocumentChunk;

public interface DocumentChunkVectorRepositoryCustom {

    List<DocumentChunk> searchByEmbedding(String embedding, int limit);
}

