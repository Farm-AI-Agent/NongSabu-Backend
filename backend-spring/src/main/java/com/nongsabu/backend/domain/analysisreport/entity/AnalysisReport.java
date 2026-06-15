package com.nongsabu.backend.domain.analysisreport.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
import com.nongsabu.backend.domain.diseaseanalysis.entity.DiseaseAnalysis;
import com.nongsabu.backend.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Consider soft delete if generated report history should remain after user deletion.
@Getter
@Builder
@Entity
@Table(name = "analysis_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AnalysisReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_analysis_id")
    private DiseaseAnalysis diseaseAnalysis;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "suspected_problem", columnDefinition = "text")
    private String suspectedProblem;

    @Column(name = "recommended_actions", columnDefinition = "text")
    private String recommendedActions;

    @Column(columnDefinition = "text")
    private String checklist;

    @Column(name = "rag_references_json", columnDefinition = "jsonb")
    private String ragReferencesJson;
}

