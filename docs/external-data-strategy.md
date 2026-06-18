# 외부 농업 데이터 연동 전략

이 문서는 `gpt-docs`의 API 문서와 현재 백엔드 구조를 기준으로, 농사부 서비스에서 외부 데이터를 어떻게 조회하고 저장할지 정리한다.

## 결론

MVP에서는 외부 데이터를 모두 LLM Tool Calling으로 직접 붙이지 않는다. Spring Boot가 외부 API Client와 저장/검색 계층을 소유하고, LLM은 백엔드가 노출한 제한된 도구만 호출하게 둔다.

- 실시간성이 중요한 데이터는 외부 API Client + 캐시 + 호출 로그로 처리한다.
- 정책/보조금/기술문서처럼 검색과 추천 품질이 중요한 데이터는 PostgreSQL 정형 테이블과 RAG 인덱스를 함께 사용한다.
- pgvector는 의미 기반 검색, OpenSearch는 키워드/BM25 검색, PostgreSQL 정형 테이블은 필터/정렬/상태 관리에 쓴다.
- 크롤링은 공식 API나 파일데이터로 부족한 지자체 지원정책 보강 단계에서만 사용한다.

## 데이터별 권장 방식

| 데이터 | 출처 | 주 용도 | 권장 방식 | 저장소 |
| --- | --- | --- | --- | --- |
| 정부24 공공서비스/혜택 | `api.odcloud.kr/api` | 농업인 지원정책, 보조금, 지자체 혜택 검색 | 주기 수집 + 사용자 조건 기반 조회 API | PostgreSQL + pgvector + OpenSearch |
| 똑똑청년농부 | `https://apis.data.go.kr/1390000/youngV2` | 청년농 정책, 교육, 사업, 사례 추천 | 주기 수집 + 상세 조회 Tool | PostgreSQL + pgvector + OpenSearch |
| 농림축산식품부 보조금 현황 파일데이터 | 파일데이터 | 정책 목록 보강 | 배치 수집 | PostgreSQL + pgvector |
| 지자체 귀농귀촌 지원정책 | 지자체 사이트/포털 | 지역별 정책 보강 | API 우선, 부족하면 크롤링 | PostgreSQL + OpenSearch |
| KAMIS 농작물 시세 | KAMIS API | 리포트의 시세/시장 맥락 | 실시간 조회 + 짧은 TTL 캐시 | PostgreSQL 캐시 선택 |
| NCPMS 병해충검색 | `http://ncpms.rda.go.kr/npmsAPI/service` | 이미지 진단명 보강, 병해충 설명/방제법 | 초기 seed/배치 수집 + 상세 조회 Tool | PostgreSQL + pgvector + OpenSearch |
| NCPMS 병해충예측지도 | NCPMS AJAX service | 지역/작물별 예측 상황 | 필요 시 실시간 Tool | 캐시 테이블 선택 |
| 농사로 농업용어사전 | `farmDic` | 초보자 용어 설명 | 실시간 조회 Tool + 빈번한 결과 캐시 | PostgreSQL 캐시 선택 |
| 농사로 품종정보 | `varietyInfo` | 작물/품종 상세 정보 | 수집 또는 상세 조회 | PostgreSQL + OpenSearch |
| 농사로 월간/원클릭/정책/기술 자료 | `monthFarmTech`, `oneClickFarmngTchnlgy`, `rdaFarmngtchnlgyInfo`, `farmTechPolicyData` | 농업 기술 문서 검색 | 배치 수집 후 RAG | PostgreSQL + pgvector + OpenSearch |
| 농사로 농약 판매가격/비료 품질검사/농기계 안전 | 각 농사로 서비스 | 특정 질문 보조 | 필요 시 Tool, 일부는 수집 | PostgreSQL 또는 캐시 |

## Tool Calling 경계

LLM에 직접 외부 URL과 키를 넘기지 않는다. 도구는 Spring Boot 내부 서비스 함수로 한정한다.

추천 도구:

- `search_support_programs(profile, crops, region, keywords)`: 저장된 정책/지원사업에서 필터 + 하이브리드 검색.
- `get_support_program_detail(programId)`: 정책 상세와 출처 반환.
- `get_market_snapshot(cropName)`: KAMIS 시세 조회. 현재 `KamisClient` Stub을 실제 구현으로 교체한다.
- `lookup_disease_guide(cropCode, diseaseName)`: NCPMS/문서 RAG에서 증상, 발생환경, 방제법 조회.
- `lookup_agri_term(keyword)`: 농사로 `farmDic` 기반 용어 설명.
- `search_farming_tech(query, cropName)`: 농사로 기술자료 RAG 검색.

도구 호출 결과는 `tool_call_log`와 `external_api_log`에 남긴다. 성공/실패, provider, endpoint, 요청 요약, 응답 preview만 저장하고 API Key는 저장하지 않는다.

## 저장소 선택

### PostgreSQL 정형 테이블

필터링, 만료일, 지역, 대상 조건, 출처 URL, 수집 상태가 필요한 데이터에 사용한다.

추천 테이블:

- `support_programs`: 지원사업 원본/정규화 데이터.
- `support_program_regions`: 시도/시군구 매핑.
- `support_program_crops`: 작물 매핑.
- `support_program_embeddings` 또는 기존 `document_chunks` 확장: 설명문 chunk 검색.
- `external_api_cache`: KAMIS, 농사로 용어 등 짧은 캐시가 필요한 조회 결과.

### pgvector

사용자 질문이 자연어이고 문서 표현이 제각각인 경우 사용한다.

- 지원사업 설명/대상/신청조건
- 농사로 기술자료
- NCPMS 병해충 증상/방제법
- 농업 용어 설명

### OpenSearch

정확한 키워드가 중요한 한국어 검색에 사용한다.

- 정책명, 지자체명, 작물명, 병해충명
- BM25 후보 검색 후 pgvector와 RRF 결합
- 현재 `RagService`의 `HYBRID` / `HYBRID_RERANK` 흐름과 잘 맞는다.

## API별 구현 메모

HWP 매뉴얼 본문과 샘플 코드를 함께 확인했다. 농사로 매뉴얼은 서비스별로 `서비스 명`, `제공방식`, `오퍼레이션`, `Request Parameters`, `Response Element`를 제공한다. 공통 결과코드는 대체로 `00` 정상, `11` 인증키 누락/오류, `12` 인증키 중지, `13` 서비스/오퍼레이션 오류, `15` AJAX 도메인 오류, `91` 시스템 오류로 정리된다.

### 농사로

샘플 코드 기준 공통 호출 패턴은 다음과 같다.

```text
http://api.nongsaro.go.kr/service/{serviceName}/{operationName}?apiKey={key}
```

확인한 주요 service/operation:

- `commonCode`: `standardTopCodeLst`, `standardSubCodeLst`, `commonTopCodeLst`, `commonMiddleCodeLst`, `commonBottomCodeLst`
- `farmDic`: `searchEqualWord`, `searchFrontMatch`, `detailWord`, `detailLikeWordList`, `thesaurusDtlTerm`, `thesaurusDtlWordTree`, `thesaurusDtlScopeNotes`
- `varietyInfo`: `insttList`, `mainCategoryList`, `middleCategoryList`, `subCategoryList`, `varietyList`
- `monthFarmTech`: `monthFarmTechLst`
- `rdaFarmngtchnlgyInfo`: `rdaFarmngtchnlgyInfoLst`
- `oneClickFarmngTchnlgy`: `oneClickFarmngTchnlgyClassSubCodeLst`
- `farmTechPolicyData`: `cropsGubunList`
- `pesticideSalePrice`: `yearGubunList`
- `machineSafety`: `machineSafetyLst`
- `agriAccident`: `agriAccidentLst`
- `openApiData`: `openApiDataList`, `openApiDataDtl`, `openApiDataOps`

농사로 API Key는 서비스별로 문서에 정리되어 있지만, 코드는 반드시 `.env` 기반 환경 변수로 주입한다.

### NCPMS

샘플 코드 기준 공통 호출 패턴은 다음과 같다.

```text
http://ncpms.rda.go.kr/npmsAPI/service?apiKey={key}&serviceCode={code}
```

확인한 serviceCode:

- `SVC01`: 병 검색
- `SVC02`: 병 상세정보
- `SVC03`: 병원체 검색
- `SVC04`: 병원체 상세정보
- `SVC09`: 잡초 검색
- `SVC11`: 사진검색 대분류
- `SVC12`: 사진검색 중분류/작물 목록
- `SVC13`: 작물별 병/해충/곤충/잡초 목록
- `SVC14`: 천적곤충 검색
- `SVC31` ~ `SVC34`: 병해충예측 관련 서비스
- `SVC41` ~ `SVC42`: 병해충예찰 관련 서비스
- `SVC51`: 병해충상담 검색

샘플 코드에서는 병 상세조회가 `SVC05 + sickKey`로 구현되어 있으며, 응답에 작물명, 병명, 전염경로, 발생환경, 증상, 방제방법, 관련 이미지가 포함된다. 이 응답은 이미지 분석 리포트의 근거 보강 데이터로 가치가 높다.

`4.OpenAPI 코드목록.xlsx`에서 확인한 포도 작물 코드는 `FT040603`이다. 현재 MVP의 포도 이미지 분석 결과와 NCPMS 병 상세 데이터를 연결하는 첫 seed로 적합하다.

PPTX 설명자료 기준 병해충검색은 REST/XML 중심이고, 병해충예측은 지도 표현 때문에 AJAX 중심이다. 예측지도는 정보제공기간에만 조회된다는 제약이 있으므로 MVP 필수 기능보다는 선택 Tool로 둔다.

### 지원사업

프론트엔드의 `SupportPrograms.vue`, `RecommendedProgramsCard.vue`는 현재 더미 배열로 동작한다. 백엔드에서 다음 API를 제공하면 그대로 교체할 수 있다.

- `GET /api/v1/support-programs?region=&cropId=&keyword=&category=`
- `GET /api/v1/support-programs/{id}`
- `GET /api/v1/support-programs/recommended`
- `POST /api/v1/support-programs/reindex`

응답은 `ApiResponse<T>`를 사용하고, Entity를 직접 반환하지 않는다.

## 크롤링 판단 기준

크롤링은 마지막 수단이다.

1. 공식 OpenAPI가 있으면 OpenAPI를 사용한다.
2. 파일데이터가 있으면 배치 다운로드로 수집한다.
3. 지자체별 HTML 페이지밖에 없고, 지원정책 품질에 꼭 필요할 때만 크롤링한다.
4. 크롤러는 Spring Boot 메인 서비스와 분리된 배치/worker로 두고, 결과만 Spring Boot API 또는 DB 적재 파이프라인으로 전달한다.
5. robots.txt, 이용약관, 요청 간격, 실패 재시도, 원문 URL/수집일 기록을 필수로 둔다.

## 구현 순서

1. `support-program` 도메인 생성: Entity, Repository, DTO, Controller, Service.
2. 정부24/똑똑청년농부 Client 인터페이스와 Stub 구현.
3. 수집 결과를 `support_programs`에 저장하고, 설명/조건 텍스트를 RAG chunk로 인덱싱.
4. 프론트 더미 지원사업 데이터를 `/api/v1/support-programs/**` 응답으로 대체할 수 있는 계약 확정.
5. `KamisClient` 실제 구현과 TTL 캐시 추가.
6. `NcpmsClient` 추가 후 포도 병 상세 seed 수집, 이미지 분석 리포트에 연결.
7. 농사로 기술자료/용어사전 Client 추가.
8. 지자체 정책이 부족하면 크롤링 worker를 별도 모듈로 추가.

## 보안/운영 주의

- `gpt-docs`의 키 파일은 로컬 참고용으로만 쓰고, 코드와 커밋에는 API Key를 남기지 않는다.
- `.env`에는 provider별 키를 분리한다.
- 외부 API 응답 원문 전체 저장은 신중하게 하고, 로그에는 preview와 상태만 남긴다.
- 수집 데이터에는 `source_provider`, `source_url`, `collected_at`, `last_seen_at`을 남겨 출처 추적이 가능하게 한다.
