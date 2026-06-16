# NongSabu Backend

초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스의 백엔드 MVP입니다.

- Spring Boot: 인증, 농장·작물 관리, 이미지 분석 연동, RAG, 리포트 생성
- FastAPI: 작물 이미지 병충해 더미 분석
- PostgreSQL + pgvector: 서비스 데이터와 임베딩 벡터 저장

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

## DB 마이그레이션

DB 스키마는 Flyway가 버전 순서대로 관리합니다. 이미 적용된 마이그레이션 파일은 수정하지 않고,
새로운 변경은 다음 버전 파일로 추가합니다.

- `V1__enable_pgvector.sql`: pgvector 확장 활성화
- `V2__create_core_domain_tables.sql`: 최초 핵심 도메인 테이블 생성
- `V3__consolidate_member_domain_tables.sql`: Member 기반 표준 테이블 생성과 레거시·중복 테이블 정리

V3는 과거 `User` 기반 컬럼인 `owner_id`, `uploaded_by`를 제거하고 모든 소유 관계를
`member_id`로 통일합니다. 사용되지 않는 중복 테이블에 데이터가 남아 있으면 데이터 유실을
방지하기 위해 마이그레이션을 중단합니다.

## 주요 API

- 인증: `/api/auth/**`
- 회원: `/api/members/**`
- 농장 프로필: `/api/farm-profiles/**`
- 작물·재배 작물: `/api/crops`, `/api/user-crops/**`
- 농장: `/api/v1/farms/**`
- 이미지 분석·SSE: `/api/v1/analysis/images/**`
- 문서·RAG: `/api/v1/documents`, `/api/v1/rag/**`
- 분석 리포트: `/api/v1/reports/**`
