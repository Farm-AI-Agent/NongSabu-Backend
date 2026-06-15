package com.nongsabu.backend.domain.externalapilog.repository;

import com.nongsabu.backend.domain.externalapilog.entity.ExternalApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalApiLogRepository extends JpaRepository<ExternalApiLog, Long> {
}

