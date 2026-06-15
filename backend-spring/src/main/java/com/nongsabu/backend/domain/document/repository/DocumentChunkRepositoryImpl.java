package com.nongsabu.backend.domain.document.repository;

import java.util.List;
import com.nongsabu.backend.domain.document.entity.DocumentChunk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentChunkRepositoryImpl implements DocumentChunkVectorRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DocumentChunk> searchByEmbedding(String embedding, int limit) {
        int safeLimit = Math.max(1, limit);
        return entityManager.createNativeQuery(
                        "SELECT * FROM document_chunks ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT " + safeLimit,
                        DocumentChunk.class
                )
                .setParameter("embedding", embedding)
                .getResultList();
    }
}
