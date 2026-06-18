package com.nongsabu.backend.infra.ai.tool;

import com.nongsabu.backend.domain.externalapilog.service.ExternalApiLogService;
import com.nongsabu.backend.infra.external.Gov24Client;
import com.nongsabu.backend.infra.external.KamisClient;
import com.nongsabu.backend.infra.external.NcpmsClient;
import com.nongsabu.backend.infra.external.NongsaroClient;
import com.nongsabu.backend.infra.external.YoungFarmerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgriToolService {

    private final KamisClient kamisClient;
    private final NongsaroClient nongsaroClient;
    private final NcpmsClient ncpmsClient;
    private final Gov24Client gov24Client;
    private final YoungFarmerClient youngFarmerClient;
    private final ExternalApiLogService externalApiLogService;

    @Tool(name = "getMarketPrice", description = "KAMIS에서 농산물 가격과 시세 정보를 조회합니다.")
    public String getMarketPrice(
            @ToolParam(description = "조회할 작물 또는 품목 이름 (예: 사과, 배추, 포도)") String cropName
    ) {
        String result = kamisClient.getMarketSnapshot(cropName);
        safeLog("getMarketPrice", cropName, result);
        return result;
    }

    @Tool(name = "searchFarmDictionary", description = "농사로 농업용어사전에서 농업 용어, 재배 방법, 작물 정보를 조회합니다.")
    public String searchFarmDictionary(
            @ToolParam(description = "검색할 농업 용어 또는 작물 이름 (예: 포도, 비료, 퇴비)") String term
    ) {
        String result = nongsaroClient.searchFarmDic(term);
        safeLog("searchFarmDictionary", term, result);
        return result;
    }

    @Tool(name = "searchDiseasePestInfo", description = "NCPMS에서 작물 병해충 정보, 증상, 방제 방법을 조회합니다.")
    public String searchDiseasePestInfo(
            @ToolParam(description = "검색할 병해충 또는 작물 이름 (예: 역병, 사과 잎말이나방, 고추)") String query
    ) {
        String result = ncpmsClient.searchDiseasePest(query);
        safeLog("searchDiseasePestInfo", query, result);
        return result;
    }

    @Tool(name = "searchGovService", description = "정부24에서 농업인 지원 정책, 보조금, 정부 서비스 목록을 조회합니다.")
    public String searchGovService(
            @ToolParam(description = "조회할 페이지 번호 (기본 1)") int page
    ) {
        String result = gov24Client.searchServicesForLlm(page, 5);
        safeLog("searchGovService", "page=" + page, result);
        return result;
    }

    @Tool(name = "searchYoungFarmerPolicy", description = "똑똑청년농부 API에서 청년 농업인 지원사업, 교육, 정책을 검색합니다.")
    public String searchYoungFarmerPolicy(
            @ToolParam(description = "검색 키워드 (예: 영농정착, 교육, 귀농, 지역명). 전체 조회는 빈 문자열 사용") String keyword
    ) {
        String result = youngFarmerClient.searchPoliciesForLlm(keyword, 1, 5);
        safeLog("searchYoungFarmerPolicy", keyword, result);
        return result;
    }

    private void safeLog(String toolName, String input, String output) {
        try {
            externalApiLogService.logToolCall(toolName, input, output);
        } catch (Exception ignored) {
        }
    }
}
