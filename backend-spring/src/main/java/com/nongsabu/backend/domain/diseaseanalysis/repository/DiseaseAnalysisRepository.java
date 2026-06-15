package com.nongsabu.backend.domain.diseaseanalysis.repository;

import java.util.List;
import com.nongsabu.backend.domain.diseaseanalysis.entity.DiseaseAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiseaseAnalysisRepository extends JpaRepository<DiseaseAnalysis, Long> {

    List<DiseaseAnalysis> findAllByMemberId(Long memberId);
}

