# 테스트용 챗봇/페이지 API 인벤토리

이 문서는 실제 `NongSabu-Frontend`와 바로 연동하기 전, Spring Boot 정적 HTML 테스트 페이지로 프론트 기능을 흉내 내며 검증할 범위를 정리한다.

목표는 다음이다.

- 백엔드만으로 로그인, 문서 업로드, RAG 검색/답변, 이미지 분석, 리포트 생성, Tool Calling 흐름을 테스트한다.
- 실제 프론트 화면과 비슷한 테스트 HTML을 `backend-spring/src/main/resources/static` 아래에 둔다.
- API Key는 `.env`/환경변수로 처리하고, HTML/JS/DB 로그에 노출하지 않는다.
- Tool Calling은 LLM에 외부 API Key를 직접 넘기지 않고 Spring Boot 내부 도구 함수로 감싼다.

## 결론

현재 상태는 “기반은 있음, 전체 구현은 아직 아님”이다.

| 항목 | 현재 상태 | 비고 |
| --- | --- | --- |
| OpenAI API Key 처리 | 구현됨 | `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `APP_CHAT_MODEL`, `APP_EMBEDDING_MODEL` 환경변수 사용 |
| KAMIS API Key 처리 | 환경변수 있음, Client는 Stub | `KAMIS_API_KEY`, `KAMIS_CUSTOMER_ID`는 설정됨. 실제 호출 구현 필요 |
| 농사로 API Key 처리 | 설계됨, 설정 미구현 | `NONGSARO_*` 환경변수 추가 필요 |
| NCPMS API Key 처리 | 설계됨, 설정 미구현 | `NCPMS_API_KEY` 환경변수 추가 필요 |
| 정부24/공공서비스 API Key 처리 | 설계됨, 설정 미구현 | `GOV_SERVICE_API_KEY` 같은 환경변수 추가 필요 |
| 똑똑청년농부 API Key 처리 | 설계됨, 설정 미구현 | `YOUNG_FARMER_API_KEY` 같은 환경변수 추가 필요 |
| 문서 업로드/RAG 조회 API | 구현됨 | `/api/v1/documents`, `/api/v1/rag/search`, `/api/v1/rag/ask` |
| RAG 테스트 HTML | 구현됨 | `/rag-test.html` |
| Tool/API 디버그 HTML | 구현됨 | `/tool-api-debug.html` |
| pgvector | 구현됨 | Spring AI VectorStore + PostgreSQL/pgvector |
| OpenSearch BM25 | 구현됨 | `Bm25SearchClient`, reindex API 있음 |
| Reranker | 구현됨 | `reranker-server`, `RerankerClient` |
| 이미지 분석 API | 구현됨 | Spring Boot -> FastAPI `/api/v1/disease/predict` |
| 병해충 지식 보강 | 필요 | NCPMS/농사로 Client 구현 필요 |
| Tool Calling 로그 | 구현됨 | `tool_call_log`, `ToolCallLogService` |
| Tool Calling 실행 | 디버그 wrapper 구현됨 | `/api/v1/debug/tools/invoke`에서 RAG/KAMIS Stub/LLM/MCP Stub 호출 가능. 실제 tool registry/orchestrator는 필요 |
| 범용 챗봇 API | 필요 | `/api/v1/chat/**` 설계 및 구현 필요 |
| 지원사업 조회 API | 필요 | 정부24/청년농부/보조금 수집 API 구현 필요 |
| 공식 문서 카탈로그 API | 필요 | `GLOBAL` 문서 scope와 `/api/v1/knowledge/**` 필요 |

## 테스트 HTML 페이지 계획

### 1. 기존 RAG 테스트 페이지

위치:

```text
backend-spring/src/main/resources/static/rag-test.html
```

테스트하는 기능:

- 회원가입: `POST /api/v1/auth/signup`
- 로그인: `POST /api/v1/auth/login`
- PDF 업로드: `POST /api/v1/documents`
- RAG 답변: `POST /api/v1/rag/ask`
- 검색 모드: `hybrid`, `hybrid-rerank`

현재 이 페이지는 문서 업로드와 RAG 검색/답변 검증에 충분하다.

### 2. 범용 챗봇 테스트 페이지

구현 파일:

```text
backend-spring/src/main/resources/static/tool-api-debug.html
```

목표:

- 실제 프론트 `Chat.vue`를 흉내 내되, 백엔드 Tool Calling 흐름을 직접 확인한다.
- 질문을 보내면 어떤 도구가 호출되었는지, 어떤 문서/외부 API가 근거로 쓰였는지 보여준다.
- API Key 설정 상태, 내부 서비스 health probe, RAG, Tool invoke, 이미지 분석을 한 페이지에서 확인한다.

구현 API:

```text
GET  /api/v1/debug/integrations/status
GET  /api/v1/debug/integrations/probe/{target}
GET  /api/v1/debug/tools/samples
POST /api/v1/debug/tools/invoke
```

Tool invoke 요청:

```json
{
  "toolName": "rag_ask",
  "query": "포도 병해 관리 방법을 알려줘.",
  "topK": 4,
  "retrievalMode": "hybrid"
}
```

Tool invoke 응답:

```json
{
  "toolName": "rag_ask",
  "success": true,
  "result": {},
  "errorMessage": null,
  "elapsedMillis": 123
}
```

현재 연결된 디버그 Tool:

- `rag_search`: `RagService.search`
- `rag_ask`: `RagService.ask`
- `kamis_market_snapshot`: `KamisClient.getMarketSnapshot`, 현재 Stub
- `llm_generate`: `LlmClient.generate`
- `mcp_stub`: `McpToolClient.invoke`, 현재 Stub

향후 실제 사용자용 챗봇은 별도 `/api/v1/chat/messages`로 만들고, 이 디버그 API에서 검증한 Tool wrapper를 옮겨 붙인다.

### 3. 병해충 분석 테스트 페이지

권장 파일:

```text
backend-spring/src/main/resources/static/disease-test.html
```

목표:

- 실제 프론트 `PestAnalysis.vue` 흐름을 백엔드에서 먼저 검증한다.
- 이미지 업로드, SSE 진행상태, 분석 결과, 리포트 생성을 한 화면에서 확인한다.

사용 API:

```text
GET  /api/v1/crops
POST /api/v1/analysis/images
GET  /api/v1/analysis/images/{imageId}
GET  /api/v1/analysis/images/{imageId}/stream
POST /api/v1/reports/images/{imageId}
```

현재 구현된 범위:

- Spring Boot가 FastAPI `/api/v1/disease/predict`를 호출한다.
- 분석 결과는 DB에 저장된다.
- SSE 진행상태 API가 있다.
- 포도 외 작물은 `UNSUPPORTED`로 처리된다.
- `/tool-api-debug.html`에서 작물 조회, 이미지 업로드, SSE fetch stream, 결과 조회, 리포트 생성을 한 번에 테스트할 수 있다.

아직 필요한 범위:

- NCPMS 병 상세 조회 Tool.
- 농사로 품종/작물/기술정보 Tool.
- 분석 결과와 외부 지식/RAG를 결합한 상세 설명 강화.

### 4. 지원사업 테스트 페이지

권장 파일:

```text
backend-spring/src/main/resources/static/support-program-test.html
```

목표:

- 실제 프론트 `SupportPrograms.vue`, `RecommendedProgramsCard.vue`의 더미 데이터를 백엔드 API로 대체하기 전에, 지원사업 검색/추천 결과를 테스트한다.

권장 API:

```text
GET  /api/v1/support-programs?region=&cropId=&keyword=&category=
GET  /api/v1/support-programs/{id}
GET  /api/v1/support-programs/recommended
POST /api/v1/support-programs/reindex
```

사용할 외부 데이터:

- 정부24 공공서비스/혜택 정보 API
- 농촌진흥청 똑똑청년농부 API
- 농림축산식품부 보조금 파일데이터
- 지자체 귀농귀촌 지원정책 데이터

현재 상태:

- 문서상 설계됨.
- 실제 Spring Boot 도메인/API/Client는 필요.

### 5. 공식 문서/지식 조회 테스트 페이지

권장 파일:

```text
backend-spring/src/main/resources/static/knowledge-test.html
```

목표:

- 사용자가 “공식문서 이런 것이 있다”는 목록을 보고, 선택한 문서/지식원으로 질문하는 흐름을 검증한다.
- 사용자 업로드 문서와 시스템 공용 공식 문서를 구분한다.

권장 API:

```text
GET  /api/v1/knowledge-sources
GET  /api/v1/knowledge-sources/{id}
POST /api/v1/knowledge/search
POST /api/v1/knowledge/ask
POST /api/v1/knowledge/reindex
```

현재 구현된 대체 API:

```text
GET  /api/v1/documents
POST /api/v1/rag/search
POST /api/v1/rag/ask
POST /api/v1/rag/opensearch/reindex
```

아직 필요한 범위:

- `GLOBAL` 또는 `SYSTEM` 문서 scope.
- `sourceType`: `NCPMS_DISEASE`, `NONGSARO_TECH`, `SUPPORT_POLICY`, `USER_UPLOAD` 등.
- 문서 출처 URL, 수집일, provider, 갱신 상태.

## API Key 처리 현황

### 이미 설정된 환경변수

`.env.example`, `docker-compose.yml`, `application.yml` 기준:

```env
OPENAI_API_KEY=replace-me
OPENAI_BASE_URL=https://api.openai.com
APP_CHAT_MODEL=gpt-4.1-mini
APP_EMBEDDING_MODEL=text-embedding-3-small
APP_EMBEDDING_DIMENSION=1536

KAMIS_API_KEY=replace-me
KAMIS_CUSTOMER_ID=replace-me
```

OpenAI Key는 embedding, RAG 답변, LLM 리포트 생성에 쓰인다.

KAMIS Key는 설정만 있고, 현재 `KamisClient` 구현체는 `StubKamisClient`다. 실제 호출 Client가 들어오면 이 환경변수를 사용한다.

### 추가해야 할 환경변수

```env
NONGSARO_API_KEY_FARM_DIC=replace-me
NONGSARO_API_KEY_VARIETY_INFO=replace-me
NONGSARO_API_KEY_COMMON_CODE=replace-me
NONGSARO_API_KEY_TECH_INFO=replace-me
NCPMS_API_KEY=replace-me
GOV_SERVICE_API_KEY=replace-me
YOUNG_FARMER_API_KEY=replace-me
```

농사로는 서비스별 키가 문서에 나뉘어 있으므로, 처음에는 필요한 서비스만 환경변수로 추가한다. 코드나 테스트 HTML에 키를 직접 넣지 않는다.

현재 로컬 `.env`에는 `gpt-docs`에서 확인 가능한 실제 키를 반영했다. 단, `KAMIS_CUSTOMER_ID`는 로컬 문서에서 별도 값을 찾지 못해 비어 있다.

## 어디서 어떤 API를 쓰는가

### OpenAI

사용 위치:

- `spring.ai.openai.api-key`
- `spring.ai.openai.chat.options.model`
- `spring.ai.openai.embedding.options.model`
- `SpringAiLlmClient`
- `RagService`
- Spring AI `VectorStore`

용도:

- 문서 chunk embedding.
- RAG 질문 답변 생성.
- 이미지 분석 리포트 텍스트 생성.
- 향후 범용 챗봇 답변 생성.

상태:

- 환경변수 처리 구현됨.
- Tool Calling orchestrator는 아직 필요.

### FastAPI 이미지 분석 서버

사용 위치:

- `FastApiAnalysisClient`
- `ImageAnalysisService`

호출 API:

```text
POST {AI_SERVER_BASE_URL}/api/v1/disease/predict
```

용도:

- 작물 이미지 병충해 분석.

상태:

- 구현됨.
- 현재 MVP는 포도만 지원.

### KAMIS

사용 위치:

- `KamisClient`
- `StubKamisClient`
- `ReportService`
- `ExternalApiLogService.logKamisMarketSnapshot`

용도:

- 농작물 시세/시장 맥락을 리포트에 붙인다.

상태:

- 환경변수 있음.
- 실제 Client 구현 필요.

### 농사로

출처:

```text
http://api.nongsaro.go.kr/service/{serviceName}/{operationName}?apiKey={key}
```

주요 사용 예정 API:

- `farmDic/searchEqualWord`
- `farmDic/detailWord`
- `varietyInfo/varietyList`
- `rdaFarmngtchnlgyInfo/rdaFarmngtchnlgyInfoLst`
- `monthFarmTech/monthFarmTechLst`
- `oneClickFarmngTchnlgy/oneClickFarmngTchnlgyClassSubCodeLst`
- `farmTechPolicyData/cropsGubunList`
- `pesticideSalePrice/pesticideSalePriceList`

용도:

- 농업 용어 설명.
- 작물/품종 정보.
- 농업 기술 문서 조회/RAG 적재.
- 농약 가격 등 보조 조회.

상태:

- `gpt-docs`에서 문서/샘플 확인 완료.
- 실제 Spring Client와 환경변수는 필요.

### NCPMS

출처:

```text
http://ncpms.rda.go.kr/npmsAPI/service?apiKey={key}&serviceCode={code}
```

주요 사용 예정 API:

- `SVC11`: 사진검색 대분류
- `SVC12`: 사진검색 중분류/작물 목록
- `SVC13`: 작물별 병/해충/곤충/잡초 목록
- `SVC05`: 병 상세조회, 샘플 코드 기준 `sickKey` 필요
- `SVC31` ~ `SVC34`: 병해충예측 관련

용도:

- 병해충 분석 결과의 증상/발생환경/방제방법 보강.
- 포도 작물 코드 `FT040603` 기반 seed 수집.
- 병해충 예측지도는 선택 Tool.

상태:

- `gpt-docs`의 HWP/PPTX/XLSX/샘플 확인 완료.
- 실제 Spring Client와 환경변수는 필요.

### 정부24 공공서비스/혜택

출처:

```text
api.odcloud.kr/api
```

용도:

- 농업인 지원정책, 보조금, 지자체 혜택 검색.
- 지원사업 추천 Tool.

상태:

- 문서상 1순위 데이터로 정리됨.
- 실제 Client, 수집 테이블, 추천 API 필요.

### 똑똑청년농부

출처:

```text
https://apis.data.go.kr/1390000/youngV2
```

용도:

- 청년농 정책, 교육, 사업, 사례 조회.

상태:

- 문서상 2순위 데이터로 정리됨.
- 실제 Client, 수집 테이블, 추천 API 필요.

### OpenSearch

사용 위치:

- `Bm25SearchClient`
- `OpenSearchReindexService`
- `RagService`

용도:

- 한국어 키워드/BM25 검색.
- pgvector 검색과 hybrid/RRF 결합.

상태:

- 구현됨.
- 테스트 HTML은 현재 `/rag-test.html`에서 RAG 답변 모드로 간접 검증 가능.

### Reranker

사용 위치:

- `reranker-server`
- `RerankerClient`
- `RagService`

용도:

- hybrid 후보 재정렬.

상태:

- 구현됨.
- `hybrid-rerank` 모드에서 사용.

## Tool Calling 처리 방식

권장 구조:

```text
ChatController
  -> ChatService
    -> ToolOrchestrator
      -> ToolRegistry
        -> RagTool
        -> SupportProgramTool
        -> DiseaseGuideTool
        -> PlantInfoTool
        -> MarketPriceTool
        -> MemberProfileTool
```

현재 있는 것:

- `McpToolClient`
- `StubMcpToolClient`
- `ToolCallLogService`
- `tool_call_log` 테이블
- `ExternalApiLogService`
- `external_api_log` 테이블
- `DebugIntegrationController`
- `DebugIntegrationService`
- `/tool-api-debug.html`

아직 필요한 것:

- `ChatController`
- `ChatService`
- `ToolRegistry`
- 실제 Tool 구현체들
- LLM 응답에서 tool request를 파싱하고 실행하는 orchestrator
- 테스트용 `chatbot-test.html`

## 문서조회 API 처리 방식

현재 구현:

```text
POST /api/v1/documents
GET  /api/v1/documents
POST /api/v1/rag/search
POST /api/v1/rag/ask
POST /api/v1/rag/opensearch/reindex
```

현재 테스트 페이지:

```text
/rag-test.html
```

현재 한계:

- 사용자 업로드 문서 중심이다.
- 농사로/NCPMS/지원정책 같은 공용 공식 문서 scope가 없다.
- 지식원 목록 조회 API가 없다.

권장 확장:

```text
GET  /api/v1/knowledge-sources
POST /api/v1/knowledge/search
POST /api/v1/knowledge/ask
```

## 구현 순서

1. `chatbot-test.html`을 먼저 만들고, `/api/v1/chat/messages` Stub API를 붙인다.
2. Stub ChatService에서 RAG 검색/답변과 ToolCallLog 저장을 연결한다.
3. `disease-test.html`을 만들고 현재 이미지 분석 API, SSE, 리포트 API를 검증한다.
4. `KamisClient` 실제 구현을 추가한다.
5. `NcpmsClient`, `NongsaroClient`를 추가하고 병해충 리포트 보강 도구로 연결한다.
6. `support-program-test.html`과 지원사업 도메인/API를 추가한다.
7. 공식문서/지식원 API를 추가하고 `knowledge-test.html`로 검증한다.

## 보안 원칙

- 테스트 HTML은 API Key를 직접 들고 있으면 안 된다.
- 외부 API 호출은 반드시 Spring Boot를 통한다.
- `tool_call_log`, `external_api_log`에는 키, 원문 민감정보, 긴 응답 전문을 저장하지 않는다.
- 로그에는 provider, endpoint, 성공 여부, 상태코드, 요청 요약, 응답 preview만 저장한다.
- `gpt-docs`의 키 파일은 로컬 참고용이고, 구현 기준은 `.env`다.
