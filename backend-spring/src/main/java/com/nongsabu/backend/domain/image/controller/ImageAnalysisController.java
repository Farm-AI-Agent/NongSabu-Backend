package com.nongsabu.backend.domain.image.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.image.dto.AnalysisResponse;
import com.nongsabu.backend.domain.image.service.AnalysisProgressBroker;
import com.nongsabu.backend.domain.image.service.ImageAnalysisService;
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
    private final AnalysisProgressBroker analysisProgressBroker;

    @PostMapping
    public ApiResponse<AnalysisResponse> uploadAndAnalyze(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long farmId,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.ok("이미지 분석이 완료되었습니다.", imageAnalysisService.uploadAndAnalyze(principal.id(), farmId, file));
    }

    @GetMapping("/{imageId}")
    public ApiResponse<AnalysisResponse> getAnalysis(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long imageId
    ) {
        return ApiResponse.ok("이미지 분석 결과 조회에 성공했습니다.", imageAnalysisService.getAnalysis(principal.id(), imageId));
    }

    @GetMapping("/{imageId}/stream")
    public SseEmitter streamProgress(@PathVariable Long imageId) {
        return analysisProgressBroker.subscribe(imageId);
    }
}
