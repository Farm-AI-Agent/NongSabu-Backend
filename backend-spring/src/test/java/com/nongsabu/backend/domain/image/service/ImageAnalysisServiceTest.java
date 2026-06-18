package com.nongsabu.backend.domain.image.service;

import static com.nongsabu.backend.support.TestFixtures.crop;
import static com.nongsabu.backend.support.TestFixtures.imageAnalysisResult;
import static com.nongsabu.backend.support.TestFixtures.member;
import static com.nongsabu.backend.support.TestFixtures.uploadedImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.image.dto.AnalysisResponse;
import com.nongsabu.backend.domain.image.entity.AnalysisStatus;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import com.nongsabu.backend.domain.image.repository.ImageAnalysisResultRepository;
import com.nongsabu.backend.domain.image.repository.UploadedImageRepository;
import com.nongsabu.backend.domain.member.service.MemberService;
import com.nongsabu.backend.infra.ai.FastApiAnalysisClient;
import com.nongsabu.backend.infra.ai.dto.AiAnalysisResponse;
import com.nongsabu.backend.infra.storage.LocalStorageService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ImageAnalysisServiceTest {

    @Mock
    private UploadedImageRepository uploadedImageRepository;

    @Mock
    private ImageAnalysisResultRepository imageAnalysisResultRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private LocalStorageService localStorageService;

    @Mock
    private FastApiAnalysisClient fastApiAnalysisClient;

    @Mock
    private AnalysisProgressBroker analysisProgressBroker;

    @InjectMocks
    private ImageAnalysisService imageAnalysisService;

    @Test
    void grapeImageCallsFastApiAndStoresCompletedResult() throws IOException {
        var member = member(1L);
        var grape = crop(10L, "\uD3EC\uB3C4");
        UploadedImage image = uploadedImage(100L, member, grape, AnalysisStatus.PENDING);
        MockMultipartFile file = imageFile();
        AiAnalysisResponse aiResponse = new AiAnalysisResponse(
                true,
                "Grape disease suspicion",
                0.87,
                "LOW",
                "summary",
                "recommendation",
                "grape-dummy-v1"
        );

        given(cropRepository.findById(10L)).willReturn(Optional.of(grape));
        given(memberService.getMember(1L)).willReturn(member);
        given(localStorageService.store(eq("images"), eq(file))).willReturn("/uploads/images/leaf.jpg");
        given(uploadedImageRepository.save(any(UploadedImage.class))).willReturn(image);
        given(fastApiAnalysisClient.analyze(file)).willReturn(aiResponse);
        given(imageAnalysisResultRepository.save(any(ImageAnalysisResult.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AnalysisResponse response = imageAnalysisService.uploadAndAnalyze(1L, 10L, file);

        assertThat(response.status()).isEqualTo(AnalysisStatus.COMPLETED.name());
        assertThat(response.supported()).isTrue();
        assertThat(response.message()).isEqualTo("이미지 분석이 완료되었습니다.");
        assertThat(response.cropName()).isEqualTo("\uD3EC\uB3C4");
        assertThat(response.diseaseName()).isEqualTo("Grape disease suspicion");
        assertThat(response.confidence()).isEqualTo(0.87);
        assertThat(response.severity()).isEqualTo("LOW");
        assertThat(image.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        verify(fastApiAnalysisClient).analyze(file);
    }

    @Test
    void grapeImageReturnsDetectionsFromFastApiResponse() throws IOException {
        var member = member(1L);
        var grape = crop(10L, "\uD3EC\uB3C4");
        UploadedImage image = uploadedImage(100L, member, grape, AnalysisStatus.PENDING);
        MockMultipartFile file = imageFile();
        AiAnalysisResponse aiResponse = new AiAnalysisResponse(
                true,
                "\uB178\uADE0\uBCD1",
                0.91,
                "HIGH",
                "\uD3EC\uB3C4 \uC78E\uC5D0\uC11C \uB178\uADE0\uBCD1\uC774 \uD0D0\uC9C0\uB418\uC5C8\uC2B5\uB2C8\uB2E4.",
                "",
                "yolo26-grape-onnx-v1",
                List.of(new AiAnalysisResponse.Detection(
                        "downy_mildew",
                        "\uB178\uADE0\uBCD1",
                        0.91,
                        List.of(120.0, 80.0, 64.0, 70.0)
                )),
                1,
                Map.of("width", 640, "height", 480)
        );

        given(cropRepository.findById(10L)).willReturn(Optional.of(grape));
        given(memberService.getMember(1L)).willReturn(member);
        given(localStorageService.store(eq("images"), eq(file))).willReturn("/uploads/images/leaf.jpg");
        given(uploadedImageRepository.save(any(UploadedImage.class))).willReturn(image);
        given(fastApiAnalysisClient.analyze(file)).willReturn(aiResponse);
        given(imageAnalysisResultRepository.save(any(ImageAnalysisResult.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AnalysisResponse response = imageAnalysisService.uploadAndAnalyze(1L, 10L, file);

        assertThat(response.detectionCount()).isEqualTo(1);
        assertThat(response.imageSize()).containsEntry("width", 640).containsEntry("height", 480);
        assertThat(response.detections()).hasSize(1);
        assertThat(response.detections().get(0).className()).isEqualTo("downy_mildew");
        assertThat(response.detections().get(0).label()).isEqualTo("\uB178\uADE0\uBCD1");
        assertThat(response.detections().get(0).bbox()).containsExactly(120.0, 80.0, 64.0, 70.0);
    }

    @Test
    void nonGrapeImageIsStoredAsUnsupportedWithoutFastApiCall() throws IOException {
        var member = member(1L);
        var tomato = crop(20L, "\uD1A0\uB9C8\uD1A0");
        UploadedImage image = uploadedImage(101L, member, tomato, AnalysisStatus.PENDING);
        MockMultipartFile file = imageFile();

        given(cropRepository.findById(20L)).willReturn(Optional.of(tomato));
        given(memberService.getMember(1L)).willReturn(member);
        given(localStorageService.store(eq("images"), eq(file))).willReturn("/uploads/images/tomato.jpg");
        given(uploadedImageRepository.save(any(UploadedImage.class))).willReturn(image);
        given(imageAnalysisResultRepository.save(any(ImageAnalysisResult.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AnalysisResponse response = imageAnalysisService.uploadAndAnalyze(1L, 20L, file);

        assertThat(response.status()).isEqualTo(AnalysisStatus.UNSUPPORTED.name());
        assertThat(response.supported()).isFalse();
        assertThat(response.message()).contains("MVP");
        assertThat(response.diseaseName()).isNull();
        assertThat(response.confidence()).isZero();
        assertThat(response.severity()).isNull();
        assertThat(response.summary()).contains("MVP");
        assertThat(image.getAnalysisStatus()).isEqualTo(AnalysisStatus.UNSUPPORTED);
        verify(fastApiAnalysisClient, never()).analyze(any());
    }

    @Test
    void fastApiFailureMarksImageAsFailedAndThrowsBusinessException() throws IOException {
        var member = member(1L);
        var grape = crop(10L, "\uD3EC\uB3C4");
        UploadedImage image = uploadedImage(100L, member, grape, AnalysisStatus.PENDING);
        MockMultipartFile file = imageFile();

        given(cropRepository.findById(10L)).willReturn(Optional.of(grape));
        given(memberService.getMember(1L)).willReturn(member);
        given(localStorageService.store(eq("images"), eq(file))).willReturn("/uploads/images/leaf.jpg");
        given(uploadedImageRepository.save(any(UploadedImage.class))).willReturn(image);
        given(fastApiAnalysisClient.analyze(file)).willThrow(new RuntimeException("ai server down"));

        assertThatThrownBy(() -> imageAnalysisService.uploadAndAnalyze(1L, 10L, file))
                .isInstanceOf(BusinessException.class);
        assertThat(image.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    @Test
    void uploadFailsWhenCropDoesNotExist() {
        given(cropRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> imageAnalysisService.uploadAndAnalyze(1L, 99L, imageFile()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getAnalysisReturnsStoredImageAndResultForOwner() {
        var member = member(1L);
        var grape = crop(10L, "\uD3EC\uB3C4");
        UploadedImage image = uploadedImage(100L, member, grape, AnalysisStatus.COMPLETED);
        ImageAnalysisResult result = imageAnalysisResult(200L, image);
        ReflectionTestUtils.setField(image, "createdAt", LocalDateTime.of(2026, 6, 17, 10, 0));
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.of(result));

        AnalysisResponse response = imageAnalysisService.getAnalysis(1L, 100L);

        assertThat(response.imageId()).isEqualTo(100L);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 17, 10, 0));
        assertThat(response.status()).isEqualTo(AnalysisStatus.COMPLETED.name());
        assertThat(response.supported()).isTrue();
        assertThat(response.diseaseName()).isEqualTo("Grape disease suspicion");
    }

    @Test
    void getAnalysesReturnsNewestImagesFirst() {
        var member = member(1L);
        var grape = crop(10L, "\uD3EC\uB3C4");
        UploadedImage newestImage = uploadedImage(200L, member, grape, AnalysisStatus.COMPLETED);
        UploadedImage olderImage = uploadedImage(100L, member, grape, AnalysisStatus.UNSUPPORTED);
        ImageAnalysisResult newestResult = imageAnalysisResult(300L, newestImage);
        ImageAnalysisResult olderResult = imageAnalysisResult(400L, olderImage);
        ReflectionTestUtils.setField(newestImage, "createdAt", LocalDateTime.of(2026, 6, 17, 11, 0));
        ReflectionTestUtils.setField(olderImage, "createdAt", LocalDateTime.of(2026, 6, 16, 9, 30));

        given(uploadedImageRepository.findAllByMemberIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(newestImage, olderImage));
        given(imageAnalysisResultRepository.findAllByUploadedImageIdIn(List.of(200L, 100L)))
                .willReturn(List.of(newestResult, olderResult));

        List<AnalysisResponse> responses = imageAnalysisService.getAnalyses(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).imageId()).isEqualTo(200L);
        assertThat(responses.get(0).createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 17, 11, 0));
        assertThat(responses.get(1).imageId()).isEqualTo(100L);
        assertThat(responses.get(1).status()).isEqualTo(AnalysisStatus.UNSUPPORTED.name());
    }

    @Test
    void subscribeProgressAllowsOnlyImageOwner() {
        var member = member(1L);
        var grape = crop(10L, "\uD3EC\uB3C4");
        UploadedImage image = uploadedImage(100L, member, grape, AnalysisStatus.PROCESSING);
        SseEmitter emitter = new SseEmitter();
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(analysisProgressBroker.subscribe(100L)).willReturn(emitter);

        SseEmitter response = imageAnalysisService.subscribeProgress(1L, 100L);

        assertThat(response).isSameAs(emitter);
        verify(analysisProgressBroker).subscribe(100L);
    }

    @Test
    void subscribeProgressRejectsOtherMemberImage() {
        given(uploadedImageRepository.findByIdAndMemberId(100L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> imageAnalysisService.subscribeProgress(2L, 100L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(analysisProgressBroker);
    }

    @Test
    void unsupportedResultRawResponseContainsSelectedCropName() throws IOException {
        var member = member(1L);
        var paprika = crop(30L, "\uD30C\uD504\uB9AC\uCE74");
        UploadedImage image = uploadedImage(102L, member, paprika, AnalysisStatus.PENDING);
        MockMultipartFile file = imageFile();
        ArgumentCaptor<ImageAnalysisResult> captor = ArgumentCaptor.forClass(ImageAnalysisResult.class);

        given(cropRepository.findById(30L)).willReturn(Optional.of(paprika));
        given(memberService.getMember(1L)).willReturn(member);
        given(localStorageService.store(eq("images"), eq(file))).willReturn("/uploads/images/paprika.jpg");
        given(uploadedImageRepository.save(any(UploadedImage.class))).willReturn(image);
        given(imageAnalysisResultRepository.save(captor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        imageAnalysisService.uploadAndAnalyze(1L, 30L, file);

        assertThat(captor.getValue().getRawResponse()).contains("\uD30C\uD504\uB9AC\uCE74");
        assertThat(captor.getValue().getRawResponse()).contains("\"supported\":false");
        assertThat(captor.getValue().getSeverity()).isNull();
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "file",
                "leaf.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );
    }
}
