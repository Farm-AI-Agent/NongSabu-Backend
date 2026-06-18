package com.nongsabu.backend.domain.document.repository;

import com.nongsabu.backend.domain.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
}
