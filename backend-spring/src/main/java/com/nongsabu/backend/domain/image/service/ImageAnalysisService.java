package com.nongsabu.backend.domain.image.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.image.dto.AnalysisResponse;
import com.nongsabu.backend.domain.image.entity.AnalysisStatus;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import com.nongsabu.backend.domain.image.repository.ImageAnalysisResultRepository;
import com.nongsabu.backend.domain.image.repository.UploadedImageRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.service.MemberService;
import com.nongsabu.backend.infra.ai.FastApiAnalysisClient;
import com.nongsabu.backend.infra.ai.dto.AiAnalysisResponse;
import com.nongsabu.backend.infra.storage.LocalStorageService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private static final String SUPPORTED_CROP_NAME = "\uD3EC\uB3C4";
    private static final String UNSUPPORTED_MESSAGE =
            "\uD604\uC7AC MVP\uC5D0\uC11C\uB294 \uD3EC\uB3C4 \uBCD1\uCDA9\uD574 \uBD84\uC11D\uB9CC \uC9C0\uC6D0\uD569\uB2C8\uB2E4.";

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageAnalysisResultRepository imageAnalysisResultRepository;
    private final CropRepository cropRepository;
    private final MemberService memberService;
    private final LocalStorageService localStorageService;
    private final FastApiAnalysisClient fastApiAnalysisClient;
    private final AnalysisProgressBroker analysisProgressBroker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AnalysisResponse uploadAndAnalyze(Long memberId, Long cropId, MultipartFile file) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "\uC791\uBB3C\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
        Member member = memberService.getMember(memberId);

        UploadedImage image = createImageEntity(member, crop, file);
        analysisProgressBroker.publish(image.getId(), AnalysisStatus.PENDING.name(), "\uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC\uAC00 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");

        if (!isSupportedCrop(crop)) {
            return markUnsupported(image, crop);
        }

        try {
            image.updateStatus(AnalysisStatus.PROCESSING);
            analysisProgressBroker.publish(image.getId(), AnalysisStatus.PROCESSING.name(), "AI \uBCD1\uCDA9\uD574 \uBD84\uC11D\uC744 \uC694\uCCAD\uD588\uC2B5\uB2C8\uB2E4.");

            AiAnalysisResponse aiResponse = fastApiAnalysisClient.analyze(file);
            ImageAnalysisResult result = imageAnalysisResultRepository.save(ImageAnalysisResult.builder()
                    .uploadedImage(image)
                    .diseaseName(aiResponse.diagnosis())
                    .confidence(aiResponse.confidence())
                    .severity(aiResponse.severity())
                    .summary(aiResponse.summary())
                    .recommendation(aiResponse.recommendedAction())
                    .rawResponse(toJson(aiResponse))
                    .build());

            image.updateStatus(AnalysisStatus.COMPLETED);
            analysisProgressBroker.publish(image.getId(), AnalysisStatus.COMPLETED.name(), "\uBD84\uC11D \uACB0\uACFC \uC800\uC7A5\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");
            return AnalysisResponse.of(image, result);
        } catch (Exception exception) {
            image.updateStatus(AnalysisStatus.FAILED);
            analysisProgressBroker.publish(image.getId(), AnalysisStatus.FAILED.name(), "\uBD84\uC11D\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "\uC774\uBBF8\uC9C0 \uBD84\uC11D \uCC98\uB9AC\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4: " + exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(Long memberId, Long imageId) {
        UploadedImage image = uploadedImageRepository.findByIdAndMemberId(imageId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "\uC774\uBBF8\uC9C0\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
        ImageAnalysisResult result = imageAnalysisResultRepository.findByUploadedImageId(imageId).orElse(null);
        return AnalysisResponse.of(image, result);
    }

    private AnalysisResponse markUnsupported(UploadedImage image, Crop crop) {
        image.updateStatus(AnalysisStatus.UNSUPPORTED);
        ImageAnalysisResult result = imageAnalysisResultRepository.save(ImageAnalysisResult.builder()
                .uploadedImage(image)
                .diseaseName(null)
                .confidence(0.0)
                .severity(AnalysisStatus.UNSUPPORTED.name())
                .summary(UNSUPPORTED_MESSAGE)
                .recommendation("\uD3EC\uB3C4 \uC791\uBB3C\uC744 \uC120\uD0DD\uD55C \uC774\uBBF8\uC9C0\uB97C \uB2E4\uC2DC \uC5C5\uB85C\uB4DC\uD574\uC8FC\uC138\uC694.")
                .rawResponse("{\"supported\":false,\"cropName\":\"" + crop.getName() + "\"}")
                .build());
        analysisProgressBroker.publish(image.getId(), AnalysisStatus.UNSUPPORTED.name(), UNSUPPORTED_MESSAGE);
        return AnalysisResponse.of(image, result);
    }

    private boolean isSupportedCrop(Crop crop) {
        return SUPPORTED_CROP_NAME.equals(crop.getName());
    }

    private UploadedImage createImageEntity(Member member, Crop crop, MultipartFile file) {
        try {
            String storedPath = localStorageService.store("images", file);
            return uploadedImageRepository.save(UploadedImage.builder()
                    .member(member)
                    .crop(crop)
                    .originalFilename(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
                    .storagePath(storedPath)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .analysisStatus(AnalysisStatus.PENDING)
                    .build());
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "\uC774\uBBF8\uC9C0 \uC800\uC7A5\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
        }
    }

    private String toJson(AiAnalysisResponse aiResponse) {
        try {
            return objectMapper.writeValueAsString(aiResponse);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
