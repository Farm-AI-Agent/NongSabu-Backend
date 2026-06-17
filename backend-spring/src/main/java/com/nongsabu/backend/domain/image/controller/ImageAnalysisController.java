package com.nongsabu.backend.domain.image.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.image.dto.AnalysisResponse;
import com.nongsabu.backend.domain.image.service.ImageAnalysisService;
import java.util.List;
import com.nongsabu.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/analysis/images")
@RequiredArgsConstructor
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;

    @PostMapping
    public ApiResponse<AnalysisResponse> uploadAndAnalyze(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long cropId,
            @RequestParam MultipartFile file
    ) {
        AnalysisResponse response = imageAnalysisService.uploadAndAnalyze(principal.id(), cropId, file);
        return ApiResponse.ok(response.message(), response);
    }

    @GetMapping
    public ApiResponse<List<AnalysisResponse>> getAnalyses(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok("이미지 분석 이력 조회에 성공했습니다.", imageAnalysisService.getAnalyses(principal.id()));
    }

    @GetMapping("/{imageId}")
    public ApiResponse<AnalysisResponse> getAnalysis(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long imageId
    ) {
        AnalysisResponse response = imageAnalysisService.getAnalysis(principal.id(), imageId);
        return ApiResponse.ok(response.message(), response);
    }

    @GetMapping("/{imageId}/stream")
    public SseEmitter streamProgress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long imageId
    ) {
        return imageAnalysisService.subscribeProgress(principal.id(), imageId);
    }
}
