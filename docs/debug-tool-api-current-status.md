# Debug Tool/API current status

이 문서는 `/tool-api-debug.html`에서 확인한 API key, endpoint, tool 샘플 결과를 기준으로 현재 잘 되는 것과 아직 빠진 것을 정리한다.

## 핵심 결론

`GET /api/v1/debug/integrations/status` 결과는 "설정값이 Spring Boot 컨테이너까지 주입되었는지"를 보여준다. 이것만으로 외부 API 호출 성공이나 실제 데이터 조회 성공까지 보장하지는 않는다.

실제 외부 API 연결 확인은 `GET /api/v1/debug/integrations/probe/{target}`로 한다. 현재 smoke test target은 다음과 같다.

| Target | 목적 | 호출 방식 |
| --- | --- | --- |
| `fastapi` | 내부 병해충 분석 서버 연결 | `/health` |
| `opensearch` | 내부 OpenSearch 연결 | OpenSearch root |
| `reranker` | 내부 reranker 연결 | `/health` |
| `kamis` | KAMIS 시세 API credential 확인 | `periodRetailProductList`, raw preview |
| `ncpms` | NCPMS 병 상세 API 연결 확인 | `SVC05`, `sickKey=D00000030`, raw preview |
| `nongsaro` | 농사로 용어사전 연결 확인 | `farmDic/searchEqualWord`, `word=포도`, raw preview |
| `gov-service` | 정부24/공공서비스 목록 API 연결 확인 | `serviceList`, raw preview |
| `young-farmer` | 똑똑청년농부 목록 API 연결 확인 | `policyList`, raw preview |

`frontend-mock-test.html`은 실제 프론트 구현물이 아니다. 프론트 화면을 만들기 위한 페이지가 아니라, 프론트 연결 전에 API 응답과 Tool-call wrapper를 한 화면에서 누르는 smoke test 콘솔이다. 병해충 진단 API와 범용 Tool-call 검증은 별도 탭으로 분리한다.

현재 상태는 다음과 같다.

| 구분 | 현재 상태 | 판단 |
| --- | --- | --- |
| OpenAI key | configured | 환경변수 주입 확인. 실제 LLM 호출은 `app.llm.enabled=true`와 `llm_generate` 결과로 별도 확인 필요 |
| KAMIS key | configured | key는 주입됨. 현재 `KamisClient`는 Stub이라 실제 KAMIS 데이터 조회는 아직 아님 |
| KAMIS customer id | missing | 로컬 `gpt-docs`에서 별도 customer/cert id 값을 찾지 못함. 실제 KAMIS 호출 구현 전 확인 필요 |
| NCPMS key | configured | key는 주입됨. Spring Client/Tool 구현은 아직 필요 |
| 농사로 key | configured | key는 주입됨. Spring Client/Tool 구현은 아직 필요 |
| 정부 서비스 key | configured | key는 주입됨. 지원사업 Client/수집 API 구현은 아직 필요 |
| 청년농부 key | configured | key는 주입됨. 지원사업 Client/수집 API 구현은 아직 필요 |
| FastAPI endpoint | base URL 표시됨 | `/probe/fastapi`로 연결 확인 필요 |
| OpenSearch endpoint | base URL 표시됨 | `/probe/opensearch`로 연결 확인 필요 |
| Reranker endpoint | base URL 표시됨 | `/probe/reranker`로 연결 확인 필요 |

## KAMIS customer id missing 의미

`KAMIS_CUSTOMER_ID`는 KAMIS 실제 API 호출에 필요할 수 있는 고객 ID 또는 인증 ID 슬롯이다. 현재 `gpt-docs/KAMIS(농작물 시세)-api-key.md`에는 API key 값만 있고 별도 customer id는 확인되지 않았다.

현재 코드에서는 `KamisClient` 구현체가 `StubKamisClient`라서 `KAMIS_CUSTOMER_ID`가 없어도 서버 실행과 디버그 페이지 동작에는 영향이 없다. 다만 실제 KAMIS 시세 API를 붙일 때는 KAMIS 문서에서 다음 중 무엇이 필요한지 확정해야 한다.

- API key만으로 호출 가능한지
- API key와 customer/cert id가 함께 필요한지
- customer/cert id가 KAMIS 계정 ID인지 별도 발급값인지

정리하면 `missing`은 현재 치명 오류가 아니라 "실제 KAMIS Client 구현 전에 확인해야 할 값"이다.

## 현재 실제로 되는 것

| 기능 | 확인 방법 | 현재 판단 |
| --- | --- | --- |
| 디버그 HTML 로딩 | `/tool-api-debug.html` 접속 | 동작 |
| 로그인 후 디버그 API 호출 | status/samples 결과 수신 | 동작 |
| API key 마스킹 표시 | status 결과 | 동작 |
| Tool 샘플 payload 제공 | `/api/v1/debug/tools/samples` | 동작 |
| RAG 검색/질문 wrapper | `rag_search`, `rag_ask` invoke | 구현됨. 문서 업로드/인덱싱 상태에 따라 결과 품질 결정 |
| FastAPI 이미지 분석 흐름 | 이미지 업로드 테스트 | 기존 구현 있음. 실제 확인은 이미지 테스트 필요 |
| OpenSearch/Reranker base URL 설정 | status 결과 | 설정됨. probe 결과로 연결 확인 필요 |
| Tool call log | `mcp_stub` 실행 | Stub MCP 호출 시 저장됨 |

## 아직 실제 데이터 조회가 아닌 것

| Tool/API | 현재 구현 | 남은 일 |
| --- | --- | --- |
| `kamis_market_snapshot` | `StubKamisClient` 더미 응답 | 실제 KAMIS HTTP Client 구현, 응답 DTO, TTL cache, 실패 로그 |
| NCPMS 병해충 상세 | key만 있음 | `NcpmsClient`, 병해충 검색/상세 Tool, 포도 코드 seed 연결 |
| 농사로 용어/품종/기술자료 | key만 있음 | `NongsaroClient`, service별 DTO, RAG 적재 또는 실시간 Tool |
| 정부24/공공서비스 지원사업 | key만 있음 | 지원사업 Entity/API/수집 Client 구현 |
| 청년농부 정책 | key만 있음 | 지원사업 수집 Client 구현 |
| MCP | `StubMcpToolClient` | 실제 MCP client 또는 내부 Tool registry 연결 |
| 범용 챗봇 | debug wrapper만 있음 | `/api/v1/chat/**`, Tool orchestrator, 대화 저장 구현 |

## 디버그 화면에서 봐야 할 순서

1. `Integration Status`
   - key가 들어왔는지만 확인한다.
   - `configured=true`는 실제 API 성공이 아니라 환경변수 주입 성공이다.

2. `Connectivity Probe`
   - `fastapi`, `opensearch`, `reranker` 각각 `success=true`인지 확인한다.
   - 실패하면 컨테이너 네트워크, 서비스 health, base URL을 먼저 본다.

3. `Tool Calling Debug`
   - `llm_generate`: LLM bean이 실제인지 stub인지 확인한다.
   - `rag_search`, `rag_ask`: 문서 업로드/인덱싱 후 검색 결과가 나오는지 확인한다.
   - `kamis_market_snapshot`: 지금은 더미 응답이 정상이다. 실제 KAMIS 데이터가 나오면 아직 구현 전 상태와 다르다.
   - `mcp_stub`: tool log 저장과 wrapper 동작 확인용이다.

4. `Image Analysis Flow`
   - 작물 목록 조회, 이미지 업로드, SSE, 결과 조회, 리포트 생성을 한 번에 확인한다.

## 다음 구현 우선순위

1. `KamisClient` 실제 구현
   - `KAMIS_API_KEY`와 `KAMIS_CUSTOMER_ID` 필요 여부 확정.
   - cropName을 KAMIS 품목 코드/이름으로 매핑.
   - 응답 DTO와 1시간 이하 TTL cache 추가.
   - `external_api_log`에 provider, endpoint, status, response preview 기록.

2. `NcpmsClient` 구현
   - 포도 코드 `FT040603`을 우선 seed로 사용.
   - 병해충 검색/상세 조회 Tool 추가.
   - 이미지 분석 결과의 diseaseName과 NCPMS 상세 설명을 연결.

3. `NongsaroClient` 구현
   - `farmDic`: 초보자 용어 설명 Tool.
   - `varietyInfo`: 작물/품종 정보 Tool.
   - 기술자료 계열은 batch 수집 후 RAG 적재 우선.

4. 지원사업 API 구현
   - `/api/v1/support-programs`
   - `/api/v1/support-programs/{id}`
   - `/api/v1/support-programs/recommended`
   - 정부 서비스/청년농부 데이터를 정형 테이블과 RAG chunk에 함께 적재.

5. 범용 챗봇 API 구현
   - `/api/v1/chat/messages`
   - `ToolRegistry`
   - `ToolOrchestrator`
   - RAG, KAMIS, NCPMS, 농사로, 지원사업 Tool을 동일한 호출/로그 포맷으로 통합.

## 현재 결과 해석 요약

지금 결과는 "백엔드가 주요 key를 잘 읽고 있고, 디버그용 tool wrapper까지 노출됐다"는 점에서는 정상이다. 하지만 "외부 농업 API에서 실제 데이터를 가져온다"는 단계는 아직 아니다.

특히 KAMIS는 key는 들어왔지만 `StubKamisClient` 상태라 실제 시세 조회가 아니다. NCPMS, 농사로, 정부 서비스, 청년농부도 key는 들어왔지만 아직 Spring Client와 Tool 구현이 필요하다.
