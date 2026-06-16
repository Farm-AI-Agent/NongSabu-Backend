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
import com.nongsabu.backend.infra.ai.llm.LlmClient;
import com.nongsabu.backend.infra.external.KamisClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
    private final LlmClient llmClient;

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
        String reportText = generateReportText(image, result, ragContext, marketContext);

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

    private String generateReportText(
            UploadedImage image,
            ImageAnalysisResult result,
            String ragContext,
            String marketContext
    ) {
        String prompt = buildLlmPrompt(image, result);
        String context = buildLlmContext(ragContext, marketContext);
        try {
            String generated = llmClient.generate(prompt, context);
            if (StringUtils.isNotBlank(generated)) {
                return generated;
            }
        } catch (Exception ignored) {
            // 로컬 MVP에서는 외부 LLM 설정이 없어도 리포트 생성 흐름을 검증할 수 있어야 한다.
        }
        return buildRuleBasedReport(result, ragContext, marketContext);
    }

    private String resolveCropName(UploadedImage image) {
        if (image.getCrop() != null) {
            return image.getCrop().getName();
        }
        return "작물";
    }

    private String buildLlmPrompt(UploadedImage image, ImageAnalysisResult result) {
        return """
                작물: %s
                진단명: %s
                신뢰도: %.2f
                위험도: %s
                분석 요약: %s
                1차 권장 조치: %s

                위 정보를 바탕으로 초보 농가가 바로 따라 할 수 있는 대처 리포트를 작성하세요.
                리포트에는 요약, 의심 원인, 즉시 조치, 관찰 체크리스트, 전문가 상담 권고를 포함하세요.
                """.formatted(
                resolveCropName(image),
                result.getDiseaseName(),
                result.getConfidence(),
                result.getSeverity(),
                result.getSummary(),
                result.getRecommendation()
        );
    }

    private String buildLlmContext(String ragContext, String marketContext) {
        return """
                [RAG 문서 근거]
                %s

                [외부 시장 정보]
                %s
                """.formatted(
                StringUtils.isBlank(ragContext) ? "관련 매뉴얼 문맥이 없습니다." : ragContext,
                marketContext
        );
    }

    private String buildRuleBasedReport(ImageAnalysisResult result, String ragContext, String marketContext) {
        return """
                [병충해 분석 요약]
                - 진단: %s
                - 신뢰도: %.2f
                - 위험도: %s

                [1차 대처 가이드]
                %s

                [RAG 매뉴얼 근거]
                %s

                [외부 시장 정보]
                %s
                """.formatted(
                result.getDiseaseName(),
                result.getConfidence(),
                result.getSeverity(),
                result.getRecommendation(),
                StringUtils.isBlank(ragContext) ? "관련 매뉴얼 문맥이 없습니다." : ragContext,
                marketContext
        );
    }
}
