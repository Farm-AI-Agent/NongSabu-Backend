# ai-server 응답 규격 (API Contract)

ai-server(FastAPI)가 Spring 백엔드의 `FastApiAnalysisClient` 에게 추론 결과를 **어떤 형식으로
응답하는지** 정의한다. 이 문서가 두 서버 간 계약의 기준이며, 필드를 바꾸려면 Spring 팀과 합의해야 한다.

> 관련 코드
> - 응답 스키마: [`app/schemas/analysis.py`](../app/schemas/analysis.py)
> - Spring 측 역직렬화: `backend-spring/.../infra/ai/dto/AiAnalysisResponse.java`

---

## 1. 통신 개요

```
[클라이언트] ──이미지──▶ [Spring :8080] ──multipart──▶ [ai-server :8000]
                              ▲                              │
                              └────────  JSON 응답  ─────────┘
```

- **프로토콜**: HTTP / `application/json`
- **인코딩**: UTF-8
- **필드명 규칙**: **snake_case** (Spring 이 `@JsonProperty` 로 매핑하므로 반드시 준수)
- **호출 방식**: Spring 이 동기(blocking)로 요청 → ai-server 는 추론 후 단일 응답 반환

---

## 2. 엔드포인트

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/api/v1/disease/predict` |
| Content-Type(요청) | `multipart/form-data` |
| 필드명(요청) | `file` (이미지 바이너리) |
| Content-Type(응답) | `application/json` |

### 부가 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| `GET` | `/health` | Liveness + 모델 적재 여부(`{status, model_loaded, mode}`). 도커 healthcheck 용 |
| `GET` | `/info` | 적재된 모델 메타데이터(버전, 클래스, 추론 파라미터) |
| `GET` | `/` | 단순 상태 메시지 |

### 요청 예시

```
POST /api/v1/disease/predict
Content-Type: multipart/form-data; boundary=...

file=<이미지 바이너리>
```

---

## 3. 성공 응답 (200 OK)

ai-server 는 **"요약 필드(Spring 계약) + 상세 배열(YOLO 확장)"** 2계층으로 응답한다.

```json
{
  "success": true,
  "diagnosis": "포도 노균병",
  "confidence": 0.91,
  "severity": "HIGH",
  "summary": "2건의 병충해가 탐지되었습니다(대표: 포도 노균병).",
  "recommended_action": "",
  "model_version": "yolo26-grape-onnx-v1",

  "detections": [
    {
      "class_name": "downy_mildew",
      "label_ko": "노균병",
      "confidence": 0.91,
      "severity": "HIGH",
      "bbox": [120.0, 88.0, 64.0, 70.0]
    },
    {
      "class_name": "anthracnose",
      "label_ko": "탄저병",
      "confidence": 0.67,
      "severity": "MEDIUM",
      "bbox": [240.0, 150.0, 40.0, 52.0]
    }
  ],
  "detection_count": 2,
  "image_size": { "width": 1280, "height": 720 },
  "inference_time_ms": 47.3
}
```

### 3-1. 요약 필드 (Spring `AiAnalysisResponse` 계약 — 변경 금지)

| 필드 | 타입 | 설명 | 산출 방식 |
|---|---|---|---|
| `success` | boolean | 추론 성공 여부 | 항상 `true`(성공 응답) |
| `diagnosis` | string | 대표 진단명(한글) | **신뢰도 최상위** 탐지의 `label_ko` |
| `confidence` | number(0~1) | 대표 신뢰도 | 신뢰도 최상위 탐지의 `confidence` |
| `severity` | string | 종합 심각도 | 탐지들 중 **가장 높은** severity (LOW/MEDIUM/HIGH) |
| `summary` | string | 사람이 읽는 요약 | 탐지 건수/대표 병명 기반 자동 생성 |
| `recommended_action` | string | 대응 방안 | **현재 빈 문자열** — 아래 4-2 참고 |
| `model_version` | string | 모델 버전 식별자 | `AI_MODEL_VERSION` 설정값 |

### 3-2. 확장 필드 (YOLO 상세 — Spring 은 무시, 하위호환)

| 필드 | 타입 | 설명 |
|---|---|---|
| `detections` | Detection[] | 탐지 목록(신뢰도 내림차순). 비었으면 `[]` |
| `detection_count` | int | `detections` 길이 |
| `image_size` | {width,height} | 원본 이미지 픽셀 크기 |
| `inference_time_ms` | number | 추론 소요(ms) |

> Spring 의 `AiAnalysisResponse` 는 record 라 **모르는 필드를 무시**한다. 따라서 확장 필드를
> 추가해도 기존 Spring 코드는 깨지지 않으며, 추후 Spring 이 `detections` 를 읽도록 확장 가능.

### 3-3. Detection 객체

| 필드 | 타입 | 설명 |
|---|---|---|
| `class_name` | string | 영문 클래스명 (모델 라벨) |
| `label_ko` | string | 한글 병명 |
| `confidence` | number(0~1) | 탐지 신뢰도 |
| `severity` | string | 클래스 기본 심각도 (LOW/MEDIUM/HIGH) |
| `bbox` | number[4] | **원본 이미지 좌표계**의 `[x, y, width, height]` (픽셀) |

- `bbox` 좌상단 원점, `x`/`y` 는 박스 좌상단, `width`/`height` 는 박스 크기.
- 라벨 매핑은 [`labels.example.json`](../labels.example.json) 형식의 `labels.json` 으로 주입.

---

## 4. 진단 결과가 없을 때 / 대응문구

### 4-1. 탐지 0건 = "정상"

이 모델의 학습 클래스는 **`노균병`, `탄저병` 2개**다. **"정상"은 별도 클래스가 아니라
탐지된 박스가 0건인 경우**로 판정한다(정상 이미지는 박스 없는 음성 샘플로 학습).

```json
{
  "success": true,
  "diagnosis": "정상",
  "confidence": 0.0,
  "severity": "LOW",
  "summary": "포도 잎에서 병충해가 탐지되지 않았습니다(정상).",
  "recommended_action": "",
  "model_version": "yolo26-grape-onnx-v1",
  "detections": [],
  "detection_count": 0,
  "image_size": { "width": 1280, "height": 720 },
  "inference_time_ms": 31.2
}
```

### 4-2. `recommended_action` 책임 분리 ⚠️ (팀 합의 필요)

YOLO 는 **병명/위치만** 판별할 수 있고 "대응 방안 텍스트"는 생성하지 못한다. 따라서 두 안 중 합의 필요:

- **(A) Spring RAG 모듈이 생성** — ai-server 는 `class_name` 만 정확히 주고, Spring 이
  pgvector + OpenAI 로 대응문구 생성 ← **권장**
- (B) ai-server 가 클래스→대응문구 매핑 테이블로 생성

현재 구현은 **(A) 전제**로 `recommended_action` 을 빈 문자열로 둔다.

---

## 5. 에러 응답

추론 실패 시 **HTTP 5xx + 에러 본문**으로 응답한다. Spring `FastApiAnalysisClient` 는 비-2xx 응답을
`BAD_GATEWAY` 로 처리하고, `ImageAnalysisService` 가 분석 상태를 `FAILED` 로 전이시킨다.

```json
{
  "success": false,
  "error_code": "INFERENCE_FAILED",
  "message": "이미지를 디코딩할 수 없습니다.",
  "model_version": "yolo26-grape-onnx-v1"
}
```

| `error_code` | 의미 | HTTP |
|---|---|---|
| `INVALID_IMAGE` | 이미지 디코딩 불가/손상 | 400 |
| `INFERENCE_FAILED` | 추론 중 내부 오류 | 500 |
| `MODEL_NOT_LOADED` | 모델 미탑재 상태에서 실모델 요구 | 503 |

> 참고: 모델 미탑재 시 현재는 에러 대신 **데모 더미 응답**(아래 6 참고)을 반환한다.

---

## 6. 데모 더미 (실모델 탑재 전 시연용)

이 서비스의 모델은 **"포도 전용"** 이다 — 포도 사진에서 `노균병`/`탄저병`을 탐지하고,
탐지가 없으면 `정상`으로 본다. 학습 데이터는 3종(정상/탄저병/노균병)을 업로드한다.
(딸기·가지 등 작물 무관 판별은 데이터셋이 500GB+ 라 MVP 비대상)

실제 `.onnx` 가 없을 때(`AI_MODEL_PATH` 미설정/파일없음) `DummyDiseasePredictor` 로 폴백하며,
**업로드 파일명 키워드로 결과를 분기**해 발표/시연에서 "포도 선택 → 추론" 흐름을 보여줄 수 있다.
응답 형식은 실모델과 **동일한 계약**을 따른다. `model_version` 에 `-demo` 접미사가 붙는다.

| 파일명 키워드 | 진단(`diagnosis`) | severity | conf | detections |
|---|---|---|---|---|
| `healthy` / `normal` / `정상` | 정상 | LOW | 0.96 | 0건 |
| `downy` / `노균` | 노균병 | HIGH | 0.92 | 1건 |
| `anthracnose` / `탄저` | 탄저병 | MEDIUM | 0.78 | 1건 |
| (그 외) | 노균병(기본) | HIGH | 0.92 | 1건 |

- 데모 박스(`bbox`)는 이미지 중앙 약 40% 영역으로 생성된다.
- 실모델 탑재 시 이 분기는 사용되지 않는다(ONNX 추론기로 대체).

---

## 7. severity / confidence 규약

- `severity` 값 집합: `LOW` | `MEDIUM` | `HIGH` (대문자 고정)
- 종합 `severity` = 탐지들 중 최고 위험도 (`HIGH > MEDIUM > LOW`)
- `confidence` 범위: `0.0 ~ 1.0` (백분율 아님)

---

## 8. 모델 출력 형태(NMS) 참고

ai-server 는 NMS-free(end-to-end) 모델과 raw 모델을 **모두** 처리하므로, 이 사항은 응답 형식에
영향을 주지 않는다(클라이언트/Spring 은 신경 쓸 필요 없음). 상세는 코드 주석 및 운영 메모 참고.

---

## 9. 변경 관리

- 요약 필드(3-1) 변경은 **Spring `AiAnalysisResponse` 와 동시 수정** 필요 → 한 PR에서 양쪽 처리(모노레포 이점).
- 확장 필드(3-2/3-3) 추가는 하위호환이므로 ai-server 단독 변경 가능.
- 필드 추가/삭제 시 이 문서를 함께 갱신한다.
