package com.nongsabu.backend.domain.agriculturedocument.repository;

import java.util.List;
import com.nongsabu.backend.domain.agriculturedocument.entity.AgricultureDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgricultureDocumentChunkRepository extends JpaRepository<AgricultureDocumentChunk, Long> {

    List<AgricultureDocumentChunk> findAllByDocumentId(Long documentId);
}

