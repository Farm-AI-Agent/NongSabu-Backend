package com.nongsabu.backend.domain.analysisreport.repository;

import java.util.List;
import com.nongsabu.backend.domain.analysisreport.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findAllByMemberId(Long memberId);
}

