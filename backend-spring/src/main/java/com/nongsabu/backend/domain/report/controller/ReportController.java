package com.nongsabu.backend.domain.report.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.report.dto.AnalysisReportResponse;
import com.nongsabu.backend.domain.report.service.ReportService;
import com.nongsabu.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/images/{imageId}")
    public ApiResponse<AnalysisReportResponse> generate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long imageId
    ) {
        return ApiResponse.ok("분석 리포트 생성이 완료되었습니다.", reportService.generate(principal.id(), imageId));
    }
}
