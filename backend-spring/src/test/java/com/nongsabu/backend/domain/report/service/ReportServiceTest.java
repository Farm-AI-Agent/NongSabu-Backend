package com.nongsabu.backend.domain.report.service;

import static com.nongsabu.backend.support.TestFixtures.crop;
import static com.nongsabu.backend.support.TestFixtures.imageAnalysisResult;
import static com.nongsabu.backend.support.TestFixtures.member;
import static com.nongsabu.backend.support.TestFixtures.uploadedImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.service.RagService;
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

    @InjectMocks
    private ReportService reportService;

    @Test
    void generateCreatesReportFromImageAnalysisRagAndMarketContext() {
        UploadedImage image = uploadedImage(100L, member(1L), crop(10L, "grape"), AnalysisStatus.COMPLETED);
        var result = imageAnalysisResult(200L, image);
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.of(result));
        given(ragService.getContextSnippets(1L, "Grape disease suspicion summary", 3))
                .willReturn(List.of("manual-context-1", "manual-context-2"));
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
        assertThat(response.ragContext()).contains("manual-context-1", "manual-context-2");
        assertThat(response.externalMarketContext()).isEqualTo("market-context");
        assertThat(response.reportText()).isEqualTo("llm-generated-report");
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
