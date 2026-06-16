package com.nongsabu.backend.domain.report.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.service.RagService;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import com.nongsabu.backend.domain.image.repository.ImageAnalysisResultRepository;
import com.nongsabu.backend.domain.image.repository.UploadedImageRepository;
import com.nongsabu.backend.domain.report.dto.AnalysisReportResponse;
import com.nongsabu.backend.domain.report.entity.AnalysisReport;
import com.nongsabu.backend.domain.report.entity.ReportStatus;
import com.nongsabu.backend.domain.report.repository.ReportAnalysisReportRepository;
import com.nongsabu.backend.infra.external.KamisClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageAnalysisResultRepository imageAnalysisResultRepository;
    private final ReportAnalysisReportRepository analysisReportRepository;
    private final RagService ragService;
    private final KamisClient kamisClient;

    @Transactional
    public AnalysisReportResponse generate(Long memberId, Long imageId) {
        UploadedImage image = uploadedImageRepository.findByIdAndMemberId(imageId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."));
        ImageAnalysisResult result = imageAnalysisResultRepository.findByUploadedImageId(imageId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없습니다."));

        List<String> ragContextList = ragService.getContextSnippets(
                memberId,
                result.getDiseaseName() + " " + result.getSummary(),
                3
        );
        String ragContext = String.join("\n---\n", ragContextList);
        String marketContext = kamisClient.getMarketSnapshot(resolveCropName(image));
        String reportText = buildReport(result, ragContext, marketContext);

        AnalysisReport report = analysisReportRepository.findByUploadedImageId(imageId)
                .orElseGet(() -> analysisReportRepository.save(AnalysisReport.builder()
                        .uploadedImage(image)
                        .reportText(reportText)
                        .ragContext(ragContext)
                        .externalMarketContext(marketContext)
                        .status(ReportStatus.GENERATED)
                        .build()));
        report.refresh(reportText, ragContext, marketContext, ReportStatus.GENERATED);

        return AnalysisReportResponse.from(report);
    }

    private String resolveCropName(UploadedImage image) {
        if (image.getCrop() != null) {
            return image.getCrop().getName();
        }
        if (image.getFarm() != null && image.getFarm().getCropSummary() != null) {
            return image.getFarm().getCropSummary();
        }
        return "작물";
    }

    private String buildReport(ImageAnalysisResult result, String ragContext, String marketContext) {
        return """
                [병충해 분석 요약]
                - 진단: %s
                - 신뢰도: %.2f
                - 심각도: %s

                [1차 대처 가이드]
                %s

                [RAG 매뉴얼 문맥]
                %s

                [외부 시장 정보]
                %s
                """.formatted(
                result.getDiseaseName(),
                result.getConfidence(),
                result.getSeverity(),
                result.getRecommendation(),
                ragContext.isBlank() ? "관련 매뉴얼 문맥이 없습니다." : ragContext,
                marketContext
        );
    }
}
