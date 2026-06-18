package com.nongsabu.backend.domain.report.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.service.RagService;
import com.nongsabu.backend.domain.externalapilog.service.ExternalApiLogService;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import com.nongsabu.backend.domain.image.repository.ImageAnalysisResultRepository;
import com.nongsabu.backend.domain.image.repository.UploadedImageRepository;
import com.nongsabu.backend.domain.report.dto.AnalysisReportResponse;
import com.nongsabu.backend.domain.report.dto.AnalysisReportResponse.DiseaseGuidanceResponse;
import com.nongsabu.backend.domain.report.entity.AnalysisReport;
import com.nongsabu.backend.domain.report.entity.ReportStatus;
import com.nongsabu.backend.domain.report.repository.ReportAnalysisReportRepository;
import com.nongsabu.backend.infra.ai.llm.LlmClient;
import com.nongsabu.backend.infra.external.KamisClient;
import java.util.List;
import java.util.Locale;
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
    private final ExternalApiLogService externalApiLogService;

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
        String cropName = resolveCropName(image);
        ExternalCallResult marketResult = getMarketContext(cropName);
        LlmGenerationResult llmResult = generateReportText(image, result, ragContext, marketResult.content());

        AnalysisReport report = analysisReportRepository.findByUploadedImageId(imageId)
                .orElseGet(() -> analysisReportRepository.save(AnalysisReport.builder()
                        .uploadedImage(image)
                        .reportText(llmResult.reportText())
                        .ragContext(ragContext)
                        .externalMarketContext(marketResult.content())
                        .status(ReportStatus.GENERATED)
                        .build()));
        report.refresh(llmResult.reportText(), ragContext, marketResult.content(), ReportStatus.GENERATED);

        externalApiLogService.logKamisMarketSnapshot(
                memberId,
                report.getId(),
                cropName,
                marketResult.success(),
                marketResult.errorMessage()
        );
        externalApiLogService.logLlmGeneration(
                memberId,
                report.getId(),
                "analysis-report",
                llmResult.prompt(),
                llmResult.context(),
                llmResult.generatedText(),
                llmResult.success(),
                llmResult.errorMessage()
        );

        DiseaseGuidanceResponse diseaseGuidance = refineDiseaseGuidance(buildDiseaseGuidance(result, ragContext));
        return AnalysisReportResponse.from(report, result.getDiseaseName(), diseaseGuidance);
    }

    private ExternalCallResult getMarketContext(String cropName) {
        try {
            return new ExternalCallResult(kamisClient.getMarketSnapshot(cropName), true, null);
        } catch (RuntimeException exception) {
            return new ExternalCallResult("시장 정보 조회에 실패했습니다: " + exception.getMessage(), false, exception.getMessage());
        }
    }

    private LlmGenerationResult generateReportText(
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
                return new LlmGenerationResult(generated, prompt, context, generated, true, null);
            }
            return new LlmGenerationResult(
                    buildRuleBasedReport(result, ragContext, marketContext),
                    prompt,
                    context,
                    generated,
                    false,
                    "LLM 응답이 비어 있습니다."
            );
        } catch (Exception ignored) {
            // 로컬 MVP에서는 외부 LLM 설정이 없어도 리포트 생성 흐름을 검증할 수 있어야 한다.
            return new LlmGenerationResult(
                    buildRuleBasedReport(result, ragContext, marketContext),
                    prompt,
                    context,
                    null,
                    false,
                    ignored.getMessage()
            );
        }
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

    private DiseaseGuidanceResponse buildDiseaseGuidance(ImageAnalysisResult result, String ragContext) {
        String diseaseName = fallback(result.getDiseaseName(), "진단명 없음");
        String diseaseInfo = fallback(
                result.getSummary(),
                extractByKeywords(ragContext, List.of(diseaseName, "증상", "병징", "피해"), "RAG 근거에서 병 정보를 찾지 못했습니다.")
        );
        String outbreakCause = extractByKeywords(
                ragContext,
                List.of("원인", "발병", "발생", "환경", "조건", "습도", "강우", "비", "온도"),
                "RAG 근거에서 발병 원인을 명확히 찾지 못했습니다."
        );
        String ragTreatment = extractByKeywords(
                ragContext,
                List.of("방제", "치료", "관리", "제거", "살포", "예방", "약제", "소독"),
                ""
        );
        String treatment = joinGuidance(result.getRecommendation(), ragTreatment);
        if (StringUtils.isBlank(treatment)) {
            treatment = "RAG 근거에서 해결방안을 명확히 찾지 못했습니다. 지역 농업기술센터 또는 전문가 상담을 권장합니다.";
        }
        return new DiseaseGuidanceResponse(
                diseaseName,
                diseaseInfo,
                outbreakCause,
                treatment,
                StringUtils.isBlank(ragContext) ? null : ragContext
        );
    }

    private DiseaseGuidanceResponse refineDiseaseGuidance(DiseaseGuidanceResponse draft) {
        if (draft == null || StringUtils.isBlank(draft.ragContext())) {
            return draft;
        }
        String prompt = """
                포도 병해 진단 결과와 RAG 문서 근거를 바탕으로 프론트에 바로 보여줄 병해 안내문을 작성하세요.
                반드시 아래 세 섹션 제목을 그대로 사용하고, 각 섹션은 초보 농가가 이해하기 쉬운 한국어 2~4문장으로 정리하세요.
                문서 근거에 없는 내용은 추측하지 말고, 불확실하면 전문가 상담 권고를 포함하세요.

                [병 정보]
                [발병 원인]
                [해결 방안]
                """;
        String context = """
                진단명: %s

                초안 병 정보:
                %s

                초안 발병 원인:
                %s

                초안 해결 방안:
                %s

                RAG 문서 근거:
                %s
                """.formatted(
                draft.diseaseName(),
                draft.diseaseInfo(),
                draft.outbreakCause(),
                draft.treatment(),
                draft.ragContext()
        );

        try {
            String generated = llmClient.generate(prompt, context);
            if (StringUtils.isBlank(generated)) {
                return draft;
            }
            return new DiseaseGuidanceResponse(
                    draft.diseaseName(),
                    sectionOrFallback(generated, "병 정보", draft.diseaseInfo()),
                    sectionOrFallback(generated, "발병 원인", draft.outbreakCause()),
                    sectionOrFallback(generated, "해결 방안", draft.treatment()),
                    draft.ragContext()
            );
        } catch (RuntimeException exception) {
            return draft;
        }
    }

    private String sectionOrFallback(String text, String sectionTitle, String fallback) {
        String extracted = extractSection(text, sectionTitle);
        return StringUtils.isBlank(extracted) ? fallback : extracted;
    }

    private String extractSection(String text, String sectionTitle) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(sectionTitle)) {
            return "";
        }
        String currentSection = null;
        StringBuilder result = new StringBuilder();
        for (String rawLine : text.lines().toList()) {
            String line = rawLine.trim();
            String normalizedTitle = normalizeSectionTitle(line);
            if (normalizedTitle != null) {
                if (sectionTitle.equals(normalizedTitle)) {
                    currentSection = normalizedTitle;
                    continue;
                }
                if (sectionTitle.equals(currentSection)) {
                    break;
                }
                currentSection = normalizedTitle;
                continue;
            }
            if (sectionTitle.equals(currentSection) && StringUtils.isNotBlank(line)) {
                if (!result.isEmpty()) {
                    result.append('\n');
                }
                result.append(line);
            }
        }
        return result.toString().trim();
    }

    private String normalizeSectionTitle(String line) {
        String normalized = line
                .replace("#", "")
                .replace("*", "")
                .replace(":", "")
                .replace("[", "")
                .replace("]", "")
                .trim();
        if (List.of("병 정보", "발병 원인", "해결 방안").contains(normalized)) {
            return normalized;
        }
        return null;
    }

    private String extractByKeywords(String text, List<String> keywords, String fallback) {
        if (StringUtils.isBlank(text)) {
            return fallback;
        }
        List<String> normalizedKeywords = keywords.stream()
                .filter(StringUtils::isNotBlank)
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .toList();
        String result = text.lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(line -> containsAny(line, normalizedKeywords))
                .distinct()
                .limit(3)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        if (StringUtils.isBlank(result)) {
            return fallback;
        }
        return result.length() <= 1200 ? result : result.substring(0, 1200);
    }

    private boolean containsAny(String line, List<String> keywords) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(normalized::contains);
    }

    private String joinGuidance(String primary, String secondary) {
        if (StringUtils.isBlank(primary)) {
            return secondary;
        }
        if (StringUtils.isBlank(secondary)) {
            return primary;
        }
        return primary.trim() + "\n" + secondary.trim();
    }

    private String fallback(String value, String fallback) {
        return StringUtils.isBlank(value) ? fallback : value;
    }

    private record ExternalCallResult(String content, boolean success, String errorMessage) {
    }

    private record LlmGenerationResult(
            String reportText,
            String prompt,
            String context,
            String generatedText,
            boolean success,
            String errorMessage
    ) {
    }
}
