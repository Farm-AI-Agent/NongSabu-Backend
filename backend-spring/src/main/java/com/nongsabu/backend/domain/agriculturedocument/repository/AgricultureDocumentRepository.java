package com.nongsabu.backend.domain.agriculturedocument.repository;

import com.nongsabu.backend.domain.agriculturedocument.entity.AgricultureDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgricultureDocumentRepository extends JpaRepository<AgricultureDocument, Long> {
}

