package com.nongsabu.backend.domain.image.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.farm.entity.Farm;
import com.nongsabu.backend.domain.farm.service.FarmService;
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

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageAnalysisResultRepository imageAnalysisResultRepository;
    private final FarmService farmService;
    private final MemberService memberService;
    private final LocalStorageService localStorageService;
    private final FastApiAnalysisClient fastApiAnalysisClient;
    private final AnalysisProgressBroker analysisProgressBroker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AnalysisResponse uploadAndAnalyze(Long memberId, Long farmId, MultipartFile file) {
        Farm farm = farmService.getOwnedFarm(memberId, farmId);
        Member member = memberService.getMember(memberId);

        UploadedImage image = createImageEntity(farm, member, file);
        analysisProgressBroker.publish(image.getId(), AnalysisStatus.PENDING.name(), "이미지 업로드가 완료되었습니다.");

        try {
            image.updateStatus(AnalysisStatus.PROCESSING);
            analysisProgressBroker.publish(image.getId(), AnalysisStatus.PROCESSING.name(), "AI 병충해 분석을 요청했습니다.");

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
            analysisProgressBroker.publish(image.getId(), AnalysisStatus.COMPLETED.name(), "분석 결과 저장이 완료되었습니다.");
            return AnalysisResponse.of(image, result);
        } catch (Exception exception) {
            image.updateStatus(AnalysisStatus.FAILED);
            analysisProgressBroker.publish(image.getId(), AnalysisStatus.FAILED.name(), "분석에 실패했습니다.");
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 분석 처리에 실패했습니다: " + exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(Long memberId, Long imageId) {
        UploadedImage image = uploadedImageRepository.findByIdAndMemberId(imageId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."));
        ImageAnalysisResult result = imageAnalysisResultRepository.findByUploadedImageId(imageId).orElse(null);
        return AnalysisResponse.of(image, result);
    }

    private UploadedImage createImageEntity(Farm farm, Member member, MultipartFile file) {
        try {
            String storedPath = localStorageService.store("images", file);
            return uploadedImageRepository.save(UploadedImage.builder()
                    .farm(farm)
                    .member(member)
                    .originalFilename(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
                    .storagePath(storedPath)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .analysisStatus(AnalysisStatus.PENDING)
                    .build());
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");
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
