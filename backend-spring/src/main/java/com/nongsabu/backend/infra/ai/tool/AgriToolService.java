package com.nongsabu.backend.infra.ai.tool;

import com.nongsabu.backend.infra.external.NcpmsClient;
import com.nongsabu.backend.infra.external.NongsaroClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgriToolService {

    private final NongsaroClient nongsaroClient;
    private final NcpmsClient ncpmsClient;

    @Tool(name = "searchFarmDictionary", description = "농사로 농업용어사전에서 농업 용어, 재배 방법, 작물 정보를 조회합니다.")
    public String searchFarmDictionary(
            @ToolParam(description = "검색할 농업 용어 또는 작물 이름 (예: 포도, 비료, 퇴비)") String term
    ) {
        return nongsaroClient.searchFarmDic(term);
    }

    @Tool(name = "searchDiseasePestInfo", description = "NCPMS에서 작물 병해충 정보, 증상, 방제 방법을 조회합니다.")
    public String searchDiseasePestInfo(
            @ToolParam(description = "검색할 병해충 또는 작물 이름 (예: 역병, 사과 잎말이나방, 고추)") String query
    ) {
        return ncpmsClient.searchDiseasePest(query);
    }
}
