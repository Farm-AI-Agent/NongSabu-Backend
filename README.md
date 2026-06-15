# NongSabu Backend MVP

초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스의 백엔드 MVP 저장소다. 프론트엔드 없이 Spring Boot API 서버, FastAPI 이미지 분석 서버, PostgreSQL + pgvector를 먼저 완성하는 것을 목표로 한다.

## 아키텍처

```text
[Future Frontend]
        |
        v
[Spring Boot API Server]
  - 인증/JWT
  - 사용자/농장/작물 관리
  - 이미지 업로드/분석 오케스트레이션
  - 문서 업로드/Chunking/Embedding/RAG
  - 리포트 생성/SSE
  - 외부 API/MCP 확장 포인트
        |
        +----> [FastAPI AI Server]
        |        - 병충해 이미지 분석
        |        - 현재는 더미 응답
        |
        +----> [PostgreSQL + pgvector]
                 - 서비스 데이터
                 - 문서 chunk / embedding
                 - 분석 리포트
```

## 저장소 구조

```text
project-root/
├── backend-spring/
├── ai-server/
├── docker-compose.yml
├── .env.example
├── README.md
└── AGENTS.md
```

## 도메인 설계

### 핵심 도메인
- `auth`: 회원가입, 로그인, JWT 발급/검증
- `user`: 사용자 프로필 관리
- `farm`: 농장 프로필 관리
- `crop`: 재배 작물 마스터 및 농장-작물 연결
- `image`: 작물 사진 업로드, 분석 요청, 결과 저장, SSE 진행 상태
- `document`: 농업 문서 업로드, 파싱, chunking, embedding, 벡터 저장
- `rag`: 매뉴얼 검색 및 문맥 구성
- `report`: 이미지 분석 + RAG 결과 + 외부 정보로 대처 리포트 생성
- `infra.external`: KAMIS 등 외부 API 연동 구조
- `infra.mcp`: MCP Client / Custom MCP Server 연동 구조

## API 목록 제안

### 인증/사용자
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`

### 농장/작물
- `POST /api/v1/farms`
- `GET /api/v1/farms`
- `GET /api/v1/farms/{farmId}`
- `PUT /api/v1/farms/{farmId}`
- `GET /api/v1/crops`
- `POST /api/v1/farms/{farmId}/crops`

### 이미지 분석
- `POST /api/v1/analysis/images`
- `GET /api/v1/analysis/images/{imageId}`
- `GET /api/v1/analysis/images/{imageId}/stream`
- `POST /api/v1/reports/images/{imageId}`

### 문서/RAG
- `POST /api/v1/documents`
- `GET /api/v1/documents`
- `POST /api/v1/rag/search`

### 운영/문서
- `GET /actuator/health`
- `GET /swagger-ui/index.html`
- `GET /v3/api-docs`

## DB 테이블 목록 제안

### 서비스 테이블
- `users`
- `farms`
- `crops`
- `farm_crops`
- `uploaded_images`
- `image_analysis_results`
- `analysis_reports`

### RAG 테이블
- `document_assets`
- `document_chunks`

### 확장/운영 보조
- `tool_execution_logs` (추후)
- `external_api_cache` (추후)

## 작업 순서 제안

1. 로컬 인프라 구성: Docker Compose, PostgreSQL + pgvector
2. Spring Boot 기본 골격: 공통 응답, 예외 처리, Swagger, Security/JWT
3. 사용자/농장/작물 CRUD
4. FastAPI 더미 분석 서버 구성
5. 이미지 업로드 및 Spring -> FastAPI 연동
6. 분석 결과 저장 및 SSE 진행 상태 스트림
7. 문서 업로드/파싱/Chunking/Embedding 더미 구현
8. pgvector 기반 RAG 검색 구조 구현
9. 분석 리포트 생성
10. 외부 API/MCP 인터페이스 정리
11. 테스트 보강

## 로컬 실행

1. `.env.example`을 복사해 `.env`를 만든다.
2. 아래 명령으로 전체 스택을 실행한다.

```bash
docker compose up --build
```

3. 접속 주소
- Spring Boot Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Spring Boot Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- FastAPI Docs: [http://localhost:8000/docs](http://localhost:8000/docs)

## 현재 MVP 구현 범위

- Spring Boot 기본 API 구조
- JWT 인증 골격
- 사용자/농장/작물 관리 API
- 이미지 업로드 및 더미 AI 분석 연동
- 문서 업로드/Chunking/Embedding/RAG 기본 구조
- 분석 리포트 생성 기본 구조
- SSE 진행 상태 스트림 기본 구조
- KAMIS/MCP 확장 포인트 인터페이스

