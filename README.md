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

이전 MVP 단계에서 사용하던 `/api/crops`, `/api/auth`, `/api/members`, `/api/farm-profiles`, `/api/user-crops` 형태의 비버전 경로는 더 이상 표준으로 사용하지 않습니다.

## DB 마이그레이션

DB 스키마는 Flyway가 버전 순서대로 관리합니다. 이미 적용된 마이그레이션 파일은 수정하지 않고, 새로운 변경은 다음 버전 파일로 추가합니다.

- `V1__enable_pgvector.sql`: pgvector 확장 활성화
- `V2__create_core_domain_tables.sql`: 최초 핵심 도메인 테이블 생성
- `V3__consolidate_member_domain_tables.sql`: Member 기반 소유 관계 통합 및 레거시 테이블 정리
- `V4__add_crop_to_uploaded_images.sql`: 이미지 업로드에 작물 선택 연결 및 초기 작물 seed 확장
- `V5__restrict_crop_seed_options.sql`: MVP 작물 선택지를 포도, 토마토, 딸기, 오이, 파프리카로 정리
- `V6__allow_unsupported_image_analysis_status.sql`: 미지원 작물 분석 상태 `UNSUPPORTED` 허용

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
