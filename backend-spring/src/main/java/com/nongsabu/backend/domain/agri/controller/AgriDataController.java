package com.nongsabu.backend.domain.agri.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.agri.dto.DiseasePestResponse;
import com.nongsabu.backend.domain.agri.dto.FarmDicResponse;
import com.nongsabu.backend.domain.agri.dto.Gov24Response;
import com.nongsabu.backend.domain.agri.dto.YoungFarmerResponse;
import com.nongsabu.backend.domain.agri.service.AgriDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agri")
@RequiredArgsConstructor
public class AgriDataController {

    private final AgriDataService agriDataService;

    // 농사로 농업용어사전: GET /api/v1/agri/farm-dic?term=포도
    @GetMapping("/farm-dic")
    public ApiResponse<FarmDicResponse> farmDic(@RequestParam String term) {
        return ApiResponse.ok("농사로 사전 조회 완료.", agriDataService.searchFarmDic(term));
    }

    // NCPMS 병해충 검색: GET /api/v1/agri/disease-pest?query=역병
    @GetMapping("/disease-pest")
    public ApiResponse<DiseasePestResponse> diseasePest(@RequestParam String query) {
        return ApiResponse.ok("NCPMS 병해충 조회 완료.", agriDataService.searchDiseasePest(query));
    }

    // 정부24 지원서비스: GET /api/v1/agri/gov-service?page=1&perPage=10
    @GetMapping("/gov-service")
    public ApiResponse<Gov24Response> govService(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage
    ) {
        return ApiResponse.ok("정부24 서비스 조회 완료.", agriDataService.searchGovService(page, perPage));
    }

    // 청년농업인 정책: GET /api/v1/agri/young-farmer?keyword=영농정착&page=1&rowCnt=10
    @GetMapping("/young-farmer")
    public ApiResponse<YoungFarmerResponse> youngFarmer(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int rowCnt
    ) {
        return ApiResponse.ok("청년농업인 정책 조회 완료.", agriDataService.searchYoungFarmerPolicy(keyword, page, rowCnt));
    }
}
