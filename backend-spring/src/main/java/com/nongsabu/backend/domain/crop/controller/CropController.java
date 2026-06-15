package com.nongsabu.backend.domain.crop.controller;

import java.util.List;
import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.crop.dto.CropResponse;
import com.nongsabu.backend.domain.crop.dto.FarmCropRequest;
import com.nongsabu.backend.domain.crop.dto.FarmCropResponse;
import com.nongsabu.backend.domain.crop.service.CropService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @GetMapping("/api/v1/crops")
    public ApiResponse<List<CropResponse>> getCrops() {
        return ApiResponse.ok("작물 목록 조회에 성공했습니다.", cropService.getAllCrops());
    }

    @PostMapping("/api/v1/farms/{farmId}/crops")
    public ApiResponse<FarmCropResponse> addFarmCrop(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long farmId,
            @Valid @RequestBody FarmCropRequest request
    ) {
        return ApiResponse.ok("농장 작물이 등록되었습니다.", cropService.registerFarmCrop(principal.id(), farmId, request));
    }
}
