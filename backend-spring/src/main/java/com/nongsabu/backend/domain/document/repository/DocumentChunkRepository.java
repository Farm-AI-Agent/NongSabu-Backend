package com.nongsabu.backend.domain.document.repository;

import java.util.List;
import com.nongsabu.backend.domain.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long>, DocumentChunkVectorRepositoryCustom {

    List<DocumentChunk> findAllByDocumentAssetId(Long documentAssetId);
}

