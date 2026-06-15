package com.nongsabu.backend.domain.report.repository;

import java.util.Optional;
import com.nongsabu.backend.domain.report.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    Optional<AnalysisReport> findByUploadedImageId(Long uploadedImageId);
}

