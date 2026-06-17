package com.nongsabu.backend.domain.agri.service;

import com.nongsabu.backend.domain.agri.dto.DiseasePestResponse;
import com.nongsabu.backend.domain.agri.dto.FarmDicResponse;
import com.nongsabu.backend.domain.agri.dto.Gov24Response;
import com.nongsabu.backend.domain.agri.dto.YoungFarmerResponse;
import com.nongsabu.backend.infra.external.Gov24Client;
import com.nongsabu.backend.infra.external.NcpmsClient;
import com.nongsabu.backend.infra.external.NongsaroClient;
import com.nongsabu.backend.infra.external.YoungFarmerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgriDataService {

    private final NongsaroClient nongsaroClient;
    private final NcpmsClient ncpmsClient;
    private final Gov24Client gov24Client;
    private final YoungFarmerClient youngFarmerClient;

    public FarmDicResponse searchFarmDic(String term) {
        return nongsaroClient.searchFarmDicStructured(term);
    }

    public DiseasePestResponse searchDiseasePest(String query) {
        return ncpmsClient.searchDiseasePestStructured(query);
    }

    public Gov24Response searchGovService(int page, int perPage) {
        return gov24Client.searchServicesStructured(page, perPage);
    }

    public YoungFarmerResponse searchYoungFarmerPolicy(String keyword, int page, int rowCnt) {
        return youngFarmerClient.searchPoliciesStructured(keyword, page, rowCnt);
    }
}
