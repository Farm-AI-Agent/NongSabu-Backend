package com.nongsabu.backend.domain.toolcalllog.repository;

import com.nongsabu.backend.domain.toolcalllog.entity.ToolCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallLogRepository extends JpaRepository<ToolCallLog, Long> {
}
