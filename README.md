# NongSabu Backend

초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스의 백엔드 MVP입니다.

- Spring Boot: 인증, 회원/농장/작물 관리, 이미지 분석 연동, RAG, 리포트 생성
- FastAPI: 작물 이미지 병충해 분석 전용 서버
- PostgreSQL + pgvector: 서비스 데이터와 RAG 벡터 저장소

## 로컬 실행

1. `.env.example`을 `.env`로 복사하고 필요한 값을 설정합니다.
2. 프로젝트 루트에서 실행합니다.

```bash
docker compose up --build
```

## 확인 주소

- Spring Boot Health: http://localhost:8080/actuator/health
- Spring Boot Swagger: http://localhost:8080/swagger-ui/index.html
- FastAPI Health: http://localhost:8000/health
- FastAPI Docs: http://localhost:8000/docs

## API 경로 표준

Spring Boot 서비스 API는 `/api/v1`을 표준 prefix로 사용합니다.

- 인증: `/api/v1/auth/**`
- 회원: `/api/v1/members/**`
- 농장 프로필: `/api/v1/farm-profiles/**`
- 작물 목록/단건: `/api/v1/crops/**`
- 내 재배 작물: `/api/v1/user-crops/**`
- 농장: `/api/v1/farms/**`
- 이미지 분석/SSE: `/api/v1/analysis/images/**`
- 문서/RAG: `/api/v1/documents`, `/api/v1/rag/**`
- 분석 리포트: `/api/v1/reports/**`

## RAG 실제 검증 흐름

RAG 업로드/검색은 OpenAI embedding과 PostgreSQL pgvector를 사용합니다.
`/api/v1/rag/ask`는 `APP_LLM_ENABLED=true`일 때 LLM 답변을 생성하고,
기본값인 `false`에서는 검색된 문서 근거를 그대로 요약해 로컬 검증이 가능하도록 동작합니다.

1. `.env`에 실제 OpenAI API Key를 설정합니다.

```env
OPENAI_API_KEY=sk-...
APP_EMBEDDING_MODEL=text-embedding-3-small
APP_EMBEDDING_DIMENSION=1536
APP_LLM_ENABLED=false
```

2. 서비스 실행 후 회원가입/로그인으로 JWT를 발급합니다.
3. PDF 문서를 업로드합니다.

```http
POST /api/v1/documents
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

4. RAG 검색을 확인합니다.

```http
POST /api/v1/rag/search
Authorization: Bearer {token}
Content-Type: application/json

{
  "query": "포도 병충해 초기 대응 방법",
  "topK": 3
}
```

5. RAG 답변 생성을 확인합니다.

```http
POST /api/v1/rag/ask
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "포도 잎에 반점이 있을 때 초보 농가가 먼저 해야 할 일은?"
}
```

6. RAG 연결 진단을 확인합니다.

```http
GET /api/v1/rag/diagnostics
Authorization: Bearer {token}
```

`ready=true`이면 embedding 호출과 pgvector 검색 경로가 정상 응답한 것입니다.
LLM 답변 생성까지 검증하려면 `.env`에 `APP_LLM_ENABLED=true`와 `APP_CHAT_MODEL`을 함께 설정합니다.

## LLM 리포트 생성

분석 리포트는 이미지 분석 결과, RAG 문맥, 외부 시장 정보를 조합합니다.

- 기본값 `APP_LLM_ENABLED=false`: 로컬 MVP 실행을 위해 규칙 기반 리포트 fallback 사용
- `APP_LLM_ENABLED=true`: Spring AI ChatClient를 통해 실제 LLM 리포트 생성 시도
- LLM 호출 실패 또는 빈 응답이면 규칙 기반 리포트로 fallback

```env
APP_LLM_ENABLED=true
APP_CHAT_MODEL=gpt-4.1-mini
OPENAI_API_KEY=sk-...
```

## DB 마이그레이션

DB 스키마는 Flyway가 버전 순서대로 관리합니다. 이미 적용된 마이그레이션 파일은 수정하지 않고, 새로운 변경은 다음 버전 파일로 추가합니다.

- `V1__enable_pgvector.sql`: pgvector 확장 활성화
- `V2__create_core_domain_tables.sql`: 최초 핵심 도메인 테이블 생성
- `V3__consolidate_member_domain_tables.sql`: Member 기반 소유 관계 통합 및 레거시 테이블 정리
- `V4__add_crop_to_uploaded_images.sql`: 이미지 업로드에 작물 선택 연결 및 초기 작물 seed 확장
- `V5__restrict_crop_seed_options.sql`: MVP 작물 선택지를 포도, 토마토, 딸기, 오이, 파프리카로 정리
- `V6__allow_unsupported_image_analysis_status.sql`: 미지원 작물 분석 상태 `UNSUPPORTED` 허용
- `V7__refine_erd_for_rag_and_external_logs.sql`: RAG chunk, 대표 작물 FK, 외부/API 도구 호출 로그 구조 보강

## 테스트

Spring Boot 테스트는 로컬 Gradle 설치가 없을 때 Docker Gradle 이미지로 실행할 수 있습니다.

```bash
docker run --rm -v ${PWD}/backend-spring:/workspace -w /workspace gradle:8.10.2-jdk17-alpine gradle test --no-daemon
```

FastAPI 테스트는 개발 의존성을 설치한 뒤 실행합니다.

```bash
cd ai-server
pip install -r requirements-dev.txt
PYTHONPATH=. pytest tests
```
