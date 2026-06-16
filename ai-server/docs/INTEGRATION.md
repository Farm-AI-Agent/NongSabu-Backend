# ai-server ↔ Spring 연동 협의 항목

ai-server(FastAPI) 담당과 Spring 백엔드 담당이 **함께 확정/개발**해야 하는 항목 정리.
ai-server 쪽은 [docs/API.md](API.md) 계약 기준으로 이미 구현되어 있으며, 아래는 **Spring 팀과의 합의가 필요한 경계면**이다.

> 원칙: ai-server 담당은 Spring 코드를 직접 수정하지 않는다. 경계(계약)만 합의하고 각자 구현한다.

---

## A. 이미 일치 — "확정"만 하면 되는 것 (코드 변경 불필요)

현재 ai-server 구현이 Spring `FastApiAnalysisClient` / `AiAnalysisResponse` 와 이미 맞는 부분. 서로 확인만.

| 항목 | 현재 값 | 비고 |
|---|---|---|
| 엔드포인트 | `POST /api/v1/disease/predict` | Spring `FastApiAnalysisClient` 와 일치 |
| 요청 형식 | `multipart/form-data`, 필드명 `file` | 일치 |
| base-url | `http://ai-server:8000` (compose) | `AI_SERVER_BASE_URL` 일치 |
| 응답 요약 필드 | `success, diagnosis, confidence, severity, summary, recommended_action, model_version` (snake_case) | `AiAnalysisResponse` record 와 1:1 |

→ **합의 결과: "위 계약 유지"만 확인하면 됨.**

---

## B. 결정 필요 — 설계 합의 (양쪽 구현에 영향)

### B-1. `recommended_action`(대응 문구) 생성 주체 ⭐ 최우선
- 현재 ai-server 는 `recommended_action` 을 **빈 문자열**로 반환(YOLO 는 병명/위치만 판별, 대응문구 생성 불가).
- Spring 의 `ImageAnalysisService` 는 `aiResponse.recommendedAction()` 을 그대로 `recommendation` 컬럼에 저장 → 지금 그대로면 **비어서 저장됨**.
- **결정 필요**: 대응 문구를 누가 채우나?
  - (A) **Spring RAG 모듈(kdh2929)** 이 병명으로 pgvector+LLM 검색해 생성 ← 권장
  - (B) ai-server 가 병명→고정 문구 매핑 테이블로 생성
- 합의에 따라: (A)면 ai-server 변경 없음 / (B)면 ai-server 에 매핑 추가.

### B-2. `detections` 확장 필드 활용 여부
- ai-server 는 박스 목록(`detections`, `detection_count`, `image_size`, `bbox`)을 추가로 내려줌.
- Spring `AiAnalysisResponse` record 는 현재 **이 필드들을 무시**(하위호환 OK).
- **결정 필요**: 박스/다중탐지를 DB 저장하거나 화면에 표시할 것인가?
  - 표시/저장하려면 → **Spring DTO·엔티티 확장 필요(Spring 작업)**. ai-server 는 이미 제공 중이라 변경 없음.
  - 불필요하면 → 현행 유지(요약 필드만 사용).

### B-3. 에러 응답 처리 방식
- ai-server 는 실패 시 **HTTP 4xx/5xx + 구조화 본문**(`{success:false, error_code, message, model_version}`) 반환.
- Spring `FastApiAnalysisClient` 는 현재 **비-2xx면 무조건 `BAD_GATEWAY`** 로 처리하고 본문(`error_code`)은 읽지 않음.
- **결정 필요**: 사용자에게 오류 사유를 구분해 보여줄 것인가?
  - 구분 필요(예: "이미지가 손상되었습니다" vs "분석 서버 오류") → **Spring 이 `error_code` 파싱하도록 확장(Spring 작업)**.
  - 불필요 → 현행(BAD_GATEWAY 일괄) 유지.
- ai-server 의 `error_code` 목록은 [API.md §5](API.md) 참고.

### B-4. 데모/더미 응답 운영 정책
- 실모델 미탑재 시 ai-server 는 `model_version` 에 **`-demo`** 접미사가 붙은 데모 응답을 반환(파일명 분기).
- **결정 필요**: Spring/클라이언트가 데모 응답을 구분하거나 거부할 것인가? (발표 전까지는 데모로 동작)

### B-5. "정상 = 탐지 0건" 의미 합의
- ai-server 는 탐지 0건이면 `diagnosis:"정상", severity:"LOW", detections:[]` 반환.
- **확인 필요**: Spring/화면에서 "정상"을 병명과 동일 흐름으로 저장/표시해도 되는지(예외 처리 불필요한지).

---

## C. 경계 파라미터 — 숫자만 맞추면 되는 것

| 항목 | ai-server 현재 | 협의 |
|---|---|---|
| 업로드 최대 크기 | `AI_MAX_UPLOAD_MB=10` | Spring 업로드 제한과 동일하게 맞출 것 |
| 허용 이미지 포맷 | `image/*` (content-type 검사) | 클라이언트 업로드 포맷(jpg/png 등) 합의 |
| 추론 타임아웃 | 서버측 제한 없음 | Spring `WebClient` 가 `.block()` 무한대기 → **타임아웃 설정 권장(Spring 작업)** |
| 동시 추론 수 | `AI_MAX_CONCURRENCY=0`(코어수) | 부하 예상치 공유 후 튜닝 |
| `severity` 값 집합 | `LOW/MEDIUM/HIGH` (대문자) | Spring 저장/표시 시 동일 enum 으로 처리하는지 확인 |

---

## D. 포도 전용 제한 (작물 게이트) — Spring 책임

- 모델은 **포도 전용**(노균병/탄저병 탐지, 무탐지=정상). 다작물 비대상(데이터셋 500GB+ 제약).
- 비포도 작물 선택 시 "MVP 에서는 포도 병충해 분석만 지원" 처리 → **Spring 에서 ai-server 호출 전 차단**하는 것을 권장.
- ai-server 는 작물 분기 책임을 갖지 않고 "포도 추론기"로 단순 유지.
- **합의 필요**: 이 게이트를 Spring 이 맡는다는 것 확정(ai-server 는 `cropName` 등 추가 입력 안 받음).

---

## 요약: 협의 우선순위

1. **B-1** 대응문구 생성 주체 (가장 시급 — 비면 리포트가 빈 채로 저장됨)
2. **D** 포도 전용 게이트 = Spring 책임 확정
3. **B-3** 에러 사유 구분 필요 여부
4. **B-2** 박스/다중탐지 표시·저장 여부
5. **C** 업로드 한도/타임아웃 숫자 맞추기

> ai-server 쪽은 위 결정에 따라 대부분 **변경 없음**이며, 변경이 필요한 경우(B-1(B), 에러코드 추가 등)만 ai-server 에서 처리한다. Spring 측 변경(DTO 확장, error_code 파싱, 타임아웃, 작물 게이트)은 Spring 담당이 진행.
