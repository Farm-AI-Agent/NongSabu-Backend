package com.nongsabu.backend.domain.report.entity;

import com.nongsabu.backend.common.entity.BaseTimeEntity;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "analysis_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AnalysisReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_image_id", unique = true)
    private UploadedImage uploadedImage;

    @Column(columnDefinition = "text")
    private String reportText;

    @Column(columnDefinition = "text")
    private String ragContext;

    @Column(columnDefinition = "text")
    private String externalMarketContext;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    public void refresh(String reportText, String ragContext, String externalMarketContext, ReportStatus status) {
        this.reportText = reportText;
        this.ragContext = ragContext;
        this.externalMarketContext = externalMarketContext;
        this.status = status;
    }
}
