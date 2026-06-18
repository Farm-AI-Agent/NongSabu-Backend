package com.nongsabu.backend.domain.externalapilog.repository;

import com.nongsabu.backend.domain.externalapilog.entity.ExternalApiLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalApiLogRepository extends JpaRepository<ExternalApiLog, Long> {
    List<ExternalApiLog> findByProviderOrderByCreatedAtDesc(String provider, Pageable pageable);
}

