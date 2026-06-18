package com.nongsabu.backend.domain.report.service;

import static com.nongsabu.backend.support.TestFixtures.crop;
import static com.nongsabu.backend.support.TestFixtures.imageAnalysisResult;
import static com.nongsabu.backend.support.TestFixtures.member;
import static com.nongsabu.backend.support.TestFixtures.uploadedImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.service.RagService;
import com.nongsabu.backend.domain.externalapilog.service.ExternalApiLogService;
import com.nongsabu.backend.domain.image.entity.AnalysisStatus;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UploadedImageRepository uploadedImageRepository;

    @Mock
    private ImageAnalysisResultRepository imageAnalysisResultRepository;

    @Mock
    private ReportAnalysisReportRepository analysisReportRepository;

    @Mock
    private RagService ragService;

    @Mock
    private KamisClient kamisClient;

    @Mock
    private LlmClient llmClient;

    @Mock
    private ExternalApiLogService externalApiLogService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void generateCreatesReportFromImageAnalysisRagAndMarketContext() {
        UploadedImage image = uploadedImage(100L, member(1L), crop(10L, "grape"), AnalysisStatus.COMPLETED);
        var result = imageAnalysisResult(200L, image);
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.of(result));
        given(ragService.getContextSnippets(1L, "Grape disease suspicion summary", 3))
                .willReturn(List.of(
                        "발병 원인: 고온다습한 환경과 강우 후 습도가 높을 때 발생이 늘어납니다.",
                        "방제 방법: 병든 잎과 과실을 제거하고 등록 약제를 살포하세요."
                ));
        given(kamisClient.getMarketSnapshot("grape")).willReturn("market-context");
        given(llmClient.generate(any(), any())).willReturn("llm-generated-report");
        given(analysisReportRepository.findByUploadedImageId(100L)).willReturn(Optional.empty());
        given(analysisReportRepository.save(any(AnalysisReport.class))).willAnswer(invocation -> {
            AnalysisReport report = invocation.getArgument(0);
            return AnalysisReport.builder()
                    .id(300L)
                    .uploadedImage(report.getUploadedImage())
                    .reportText(report.getReportText())
                    .ragContext(report.getRagContext())
                    .externalMarketContext(report.getExternalMarketContext())
                    .status(report.getStatus())
                    .build();
        });

        AnalysisReportResponse response = reportService.generate(1L, 100L);

        assertThat(response.reportId()).isEqualTo(300L);
        assertThat(response.imageId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ReportStatus.GENERATED.name());
        assertThat(response.ragContext()).contains("발병 원인", "방제 방법");
        assertThat(response.externalMarketContext()).isEqualTo("market-context");
        assertThat(response.reportText()).isEqualTo("llm-generated-report");
        assertThat(response.diseaseName()).isEqualTo("Grape disease suspicion");
        assertThat(response.diseaseGuidance().diseaseInfo()).isEqualTo("summary");
        assertThat(response.diseaseGuidance().outbreakCause()).contains("고온다습한 환경");
        assertThat(response.diseaseGuidance().treatment()).contains("recommendation", "등록 약제");
        verify(externalApiLogService).logKamisMarketSnapshot(1L, 300L, "grape", true, null);
        verify(externalApiLogService).logLlmGeneration(
                eq(1L),
                eq(300L),
                eq("analysis-report"),
                any(),
                any(),
                eq("llm-generated-report"),
                eq(true),
                isNull()
        );
    }

    @Test
    void generateRefreshesExistingReport() {
        UploadedImage image = uploadedImage(100L, member(1L), crop(10L, "grape"), AnalysisStatus.COMPLETED);
        var result = imageAnalysisResult(200L, image);
        AnalysisReport existing = AnalysisReport.builder()
                .id(300L)
                .uploadedImage(image)
                .reportText("old")
                .ragContext("old")
                .externalMarketContext("old")
                .status(ReportStatus.FAILED)
                .build();

        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.of(result));
        given(ragService.getContextSnippets(1L, "Grape disease suspicion summary", 3)).willReturn(List.of());
        given(kamisClient.getMarketSnapshot("grape")).willReturn("new-market-context");
        given(llmClient.generate(any(), any())).willReturn("");
        given(analysisReportRepository.findByUploadedImageId(100L)).willReturn(Optional.of(existing));

        AnalysisReportResponse response = reportService.generate(1L, 100L);

        assertThat(response.reportId()).isEqualTo(300L);
        assertThat(response.status()).isEqualTo(ReportStatus.GENERATED.name());
        assertThat(response.externalMarketContext()).isEqualTo("new-market-context");
        assertThat(existing.getReportText()).contains("Grape disease suspicion");
    }

    @Test
    void generateFallsBackToRuleBasedReportWhenLlmFails() {
        UploadedImage image = uploadedImage(100L, member(1L), crop(10L, "grape"), AnalysisStatus.COMPLETED);
        var result = imageAnalysisResult(200L, image);
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.of(result));
        given(ragService.getContextSnippets(1L, "Grape disease suspicion summary", 3))
                .willReturn(List.of("manual-context"));
        given(kamisClient.getMarketSnapshot("grape")).willReturn("market-context");
        given(llmClient.generate(any(), any())).willThrow(new RuntimeException("llm down"));
        given(analysisReportRepository.findByUploadedImageId(100L)).willReturn(Optional.empty());
        given(analysisReportRepository.save(any(AnalysisReport.class))).willAnswer(invocation -> {
            AnalysisReport report = invocation.getArgument(0);
            return AnalysisReport.builder()
                    .id(300L)
                    .uploadedImage(report.getUploadedImage())
                    .reportText(report.getReportText())
                    .ragContext(report.getRagContext())
                    .externalMarketContext(report.getExternalMarketContext())
                    .status(report.getStatus())
                    .build();
        });

        AnalysisReportResponse response = reportService.generate(1L, 100L);

        assertThat(response.reportText()).contains("Grape disease suspicion", "manual-context", "market-context");
    }

    @Test
    void generateFailsWhenImageIsMissingOrNotOwned() {
        given(uploadedImageRepository.findByIdAndMemberId(100L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.generate(2L, 100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void generateFailsWhenAnalysisResultDoesNotExist() {
        UploadedImage image = uploadedImage(100L, member(1L), crop(10L, "grape"), AnalysisStatus.COMPLETED);
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.generate(1L, 100L))
                .isInstanceOf(BusinessException.class);
    }
}
