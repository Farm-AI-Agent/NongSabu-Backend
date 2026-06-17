# 프론트 기능별 챗봇/Tool Calling 검토

이 문서는 `NongSabu-Frontend`의 현재 화면과 `NongSabu-Backend`의 API를 대조해, 범용 챗봇과 병해충 특화 LLM/API 흐름이 충분히 고려되었는지 검토한 결과다. 지금 당장 실제 프론트와 직접 연동하는 것이 목표는 아니며, 먼저 Spring Boot 정적 테스트 HTML에서 프론트 흐름을 흉내 내며 백엔드 API와 Tool Calling을 검증한다.

## 요약

현재 백엔드는 이미지 분석, RAG 검색/질의, 리포트 생성의 기본 골격은 갖고 있다. 다만 프론트의 주요 화면은 아직 더미 데이터와 `localStorage` 중심이라, 실제 프론트 연동 전에는 백엔드 테스트 HTML로 같은 흐름을 먼저 검증해야 한다. Tool Calling을 활용하는 범용 챗봇과 프론트 기능 대응 API는 아직 추가 구현이 필요하다.

가장 중요한 보완점은 두 가지다.

- 범용 챗봇용 `chat` 도메인/API가 필요하다. 이 챗봇은 정책, RAG 문서, 병해충, 작물/품종, 시세, 사용자 농장 정보를 도구로 호출해야 한다.
- 병해충 특화 흐름은 FastAPI 이미지 분석 결과에 NCPMS 병해충 상세, 농사로 작물/품종/기술자료, RAG 문서를 붙여 리포트와 후속 상담으로 이어져야 한다.

## 현재 상태

### 프론트

- `Chat.vue`: 화면은 있으나 더미 메시지만 사용한다. API 호출, 스트리밍, 세션 저장, Tool Calling 결과 표시는 없다.
- `PestAnalysis.vue`: 이미지 파일을 브라우저에서 미리보기만 하고, 실제 `/api/v1/analysis/images` 호출은 없다. 진단 결과도 더미이고 이력은 `localStorage`에 저장한다.
- `DiagnosisHistory.vue`: 백엔드 진단 이력 API가 아니라 `localStorage`를 읽는다.
- `SupportPrograms.vue`, `RecommendedProgramsCard.vue`: 지원사업 더미 배열을 사용한다. 정부24/똑똑청년농부/보조금 데이터와 연결되지 않았다.
- `MyPage.vue`: 사용자/농장/작물 프로필을 `localStorage`로 관리한다. 백엔드의 member/farm-profile/user-crops API를 아직 쓰지 않는다.

### 백엔드

- 있음:
  - `POST /api/v1/analysis/images`
  - `GET /api/v1/analysis/images/{imageId}`
  - `GET /api/v1/analysis/images/{imageId}/stream`
  - `POST /api/v1/reports/images/{imageId}`
  - `POST /api/v1/rag/search`
  - `POST /api/v1/rag/ask`
  - `GET /api/v1/documents`
  - `POST /api/v1/documents`
  - member/farm-profile/farms/crops/user-crops API
- 일부만 있음:
  - `McpToolClient`, `ToolCallLog`는 Stub/로그 구조만 있다.
  - `KamisClient`는 Stub이다.
  - OpenSearch/pgvector 기반 RAG 골격은 있으나, 공식 농업 문서를 전체 사용자 공용 지식으로 적재하는 구조는 아직 명확하지 않다.
- 없음:
  - 범용 챗봇 API
  - 지원사업 조회/추천 API
  - 진단 이력 목록 API
  - NCPMS 병해충 상세 Client
  - 농사로 작물/품종/기술정보 Client
  - Tool registry/orchestrator

## 권장 챗봇 구조

### 1. 범용 농업 챗봇

프론트 `Chat.vue`는 병해충 전용 문구보다 범용 농업 비서로 두는 것이 좋다. 이 챗봇은 사용자의 질문 의도를 분류하고, 필요한 도구를 호출한 뒤 답변을 생성한다.

권장 API:

```text
POST /api/v1/chat/sessions
GET  /api/v1/chat/sessions
GET  /api/v1/chat/sessions/{sessionId}/messages
POST /api/v1/chat/sessions/{sessionId}/messages
GET  /api/v1/chat/sessions/{sessionId}/stream
```

최소 MVP는 세션 없이 다음 하나로 시작해도 된다.

```text
POST /api/v1/chat/messages
```

요청 예시:

```json
{
  "message": "전북 김제에서 포도 농사 시작했는데 받을 수 있는 지원사업 알려줘",
  "mode": "GENERAL",
  "context": {
    "currentPage": "support-programs"
  }
}
```

응답 예시:

```json
{
  "answer": "김제 지역과 포도 작물 기준으로 우선 확인할 만한 사업은 ...",
  "toolCalls": [
    {
      "toolName": "search_support_programs",
      "success": true
    }
  ],
  "sources": [
    {
      "title": "지원사업명",
      "sourceProvider": "정부24",
      "sourceUrl": "..."
    }
  ]
}
```

### 2. 병해충 특화 LLM 흐름

`PestAnalysis.vue`는 범용 챗봇과 분리된 특화 워크플로우로 처리한다. 이미지 진단은 “도구 중 하나”라기보다 사용자가 명시적으로 실행하는 분석 작업이다.

권장 흐름:

1. 프론트가 `GET /api/v1/crops`로 작물 목록 조회.
2. 이미지와 `cropId`를 `POST /api/v1/analysis/images`로 업로드.
3. 필요하면 `GET /api/v1/analysis/images/{imageId}/stream`으로 진행 상태 표시.
4. 백엔드가 FastAPI `/api/v1/disease/predict` 호출.
5. 백엔드가 결과 저장.
6. 백엔드가 NCPMS/농사로/RAG를 조회해 진단 설명을 보강.
7. 프론트가 `POST /api/v1/reports/images/{imageId}`로 사용자용 리포트 생성.
8. 사용자가 후속 질문을 하면 범용 챗봇에 `mode=PEST_ANALYSIS`와 `imageId`를 넘긴다.

권장 후속 질문 요청:

```json
{
  "message": "이 병이면 지금 농약을 바로 쳐야 해?",
  "mode": "PEST_ANALYSIS",
  "context": {
    "imageId": 123
  }
}
```

## 권장 Tool 목록

범용 챗봇과 병해충 특화 챗봇이 공유할 수 있는 도구:

- `get_member_profile`: 회원/지역/농장 규모/경험 수준 조회.
- `get_user_crops`: 사용자가 재배하거나 관심 있는 작물 조회.
- `search_support_programs`: 정부24, 똑똑청년농부, 보조금 데이터를 기반으로 지원사업 검색.
- `get_support_program_detail`: 지원사업 상세, 신청 조건, 출처 URL 조회.
- `search_rag_documents`: 업로드/수집 문서에서 하이브리드 검색.
- `ask_rag_documents`: 문서 근거 기반 답변 생성.
- `get_market_snapshot`: KAMIS 시세 조회.
- `lookup_disease_guide`: NCPMS 병해충 상세 조회.
- `lookup_plant_or_variety_info`: 농사로 품종/작물 정보 조회.
- `lookup_agri_term`: 농사로 농업용어사전 조회.
- `generate_analysis_report`: 이미지 분석 결과 + RAG + 외부 데이터 기반 리포트 생성.

도구는 LLM에 외부 URL/API Key를 직접 주지 않고 Spring Boot Service 메서드로 감싼다. 각 호출은 `tool_call_log`와 `external_api_log`에 남긴다.

## 프론트 화면별 필요한 API

| 프론트 화면 | 현재 상태 | 필요한 백엔드 API | Tool Calling 고려 |
| --- | --- | --- | --- |
| `Chat.vue` | 더미 응답 | `POST /api/v1/chat/messages`, stream 선택 | 모든 도구의 진입점 |
| `PestAnalysis.vue` | 로컬 미리보기/더미 진단 | `GET /api/v1/crops`, `POST /api/v1/analysis/images`, SSE, `POST /api/v1/reports/images/{imageId}` | `lookup_disease_guide`, `lookup_plant_or_variety_info`, `search_rag_documents` |
| `DiagnosisHistory.vue` | `localStorage` | `GET /api/v1/analysis/images`, `GET /api/v1/reports` 또는 history API | 진단 이력 기반 후속 상담 |
| `SupportPrograms.vue` | 더미 배열 | `GET /api/v1/support-programs`, `GET /api/v1/support-programs/{id}` | `search_support_programs`, `get_support_program_detail` |
| `RecommendedProgramsCard.vue` | 더미 추천 | `GET /api/v1/support-programs/recommended` | 사용자 프로필 + 지원사업 검색 |
| `MyPage.vue` | `localStorage` | `GET/PUT /api/v1/members/me`, `GET/PUT /api/v1/farm-profiles/me`, `GET/POST/DELETE /api/v1/user-crops` | 챗봇 개인화 context |
| `Dashboard.vue` | 카드 조합 | 대시보드 요약 API 선택 | 지원사업, 체크리스트, 진단 상태 요약 |

## 문서조회 API 검토

현재 `POST /api/v1/rag/search`, `POST /api/v1/rag/ask`는 이미 존재하므로 문서조회 API의 핵심은 있다. 그러나 다음 보완이 필요하다.

1. 공식/수집 문서와 사용자 업로드 문서의 범위를 분리해야 한다.
   - 현재 RAG 검색은 `memberId` 필터 중심이다.
   - 농사로/NCPMS/지원정책처럼 모든 사용자에게 필요한 문서는 `GLOBAL` 또는 `SYSTEM` scope가 필요하다.
2. 문서 출처/종류 필터가 필요하다.
   - 예: `sourceType=SUPPORT_POLICY`, `NCPMS_DISEASE`, `NONGSARO_TECH`, `USER_UPLOAD`
3. 프론트에서 “공식문서 목록 페이지”를 만들려면 단순 문서 목록보다 검색 가능한 catalog API가 필요하다.

권장 API:

```text
GET  /api/v1/knowledge-sources
GET  /api/v1/knowledge-sources/{id}
POST /api/v1/knowledge/search
POST /api/v1/knowledge/ask
POST /api/v1/knowledge/reindex
```

기존 `/api/v1/rag/**`를 내부 검색 API로 유지하고, 프론트에는 `/api/v1/knowledge/**`처럼 더 도메인 친화적인 API를 노출하는 방식이 좋다.

## 구현 우선순위

1. `Chat.vue`용 범용 챗봇 API Stub부터 추가한다.
   - 처음에는 RAG + 사용자 프로필 + 지원사업 Stub만 연결해도 충분하다.
2. `PestAnalysis.vue`를 실제 이미지 분석 API에 연결한다.
   - 작물 목록은 백엔드 `GET /api/v1/crops`를 사용한다.
   - 진단 결과는 `AnalysisResponse`로 렌더링한다.
3. `DiagnosisHistory.vue`를 백엔드 이력 API로 바꾼다.
4. 지원사업 도메인/API를 추가하고 프론트 더미 데이터를 교체한다.
5. NCPMS/농사로 Client를 붙여 병해충 리포트 보강 도구를 만든다.
6. 공식 농업 문서/정책 데이터를 `GLOBAL` RAG 문서로 적재한다.
7. Tool registry와 LLM orchestrator를 추가한다.

## 판정

Tool Calling과 문서조회 API는 일부 고려되어 있다. `ToolCallLog`, `McpToolClient`, `RagService`, `ExternalApiLog`, pgvector/OpenSearch 구조는 방향이 맞다.

하지만 프론트 기능에 대응되는 실제 API 계약은 아직 충분하지 않다. 특히 범용 챗봇, 지원사업, 진단 이력, 공식 문서 카탈로그, NCPMS/농사로 Client가 빠져 있어 현재 상태로는 “모든 데이터와 Tool Calling을 잘 활용하는 챗봇”이라고 보기 어렵다.
