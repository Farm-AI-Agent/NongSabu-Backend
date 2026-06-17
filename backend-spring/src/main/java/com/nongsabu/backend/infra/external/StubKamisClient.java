package com.nongsabu.backend.infra.external;

public class StubKamisClient implements KamisClient {

    @Override
    public String getMarketSnapshot(String cropName) {
        return cropName + "의 시장 정보 연동은 아직 더미 응답을 사용합니다. KAMIS API client 구현체로 교체 가능합니다.";
    }
}

