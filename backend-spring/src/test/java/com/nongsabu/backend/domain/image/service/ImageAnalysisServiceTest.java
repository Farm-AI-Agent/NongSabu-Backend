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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
        given(uploadedImageRepository.findByIdAndMemberId(100L, 1L)).willReturn(Optional.of(image));
        given(imageAnalysisResultRepository.findByUploadedImageId(100L)).willReturn(Optional.of(result));

        AnalysisResponse response = imageAnalysisService.getAnalysis(1L, 100L);

        assertThat(response.imageId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(AnalysisStatus.COMPLETED.name());
        assertThat(response.supported()).isTrue();
        assertThat(response.diseaseName()).isEqualTo("Grape disease suspicion");
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
