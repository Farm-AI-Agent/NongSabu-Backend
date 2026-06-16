# AGENTS

이 파일은 Codex가 이 저장소에서 작업할 때 따라야 하는 개발 규칙입니다.

## 1. 프로젝트 개요

- 이 프로젝트는 초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스입니다.
- Spring Boot 서버가 메인 백엔드 역할을 합니다.
- FastAPI 서버는 작물 이미지 병충해 분석만 담당합니다.
- PostgreSQL + pgvector는 서비스 데이터와 RAG 벡터 저장소로 사용합니다.
- 프론트엔드는 현재 구현하지 않습니다.

## 2. 구현 범위

포함합니다.

- 인증 / 사용자 / 농장 프로필
- 작물 선택
- 작물 이미지 업로드
- FastAPI 이미지 분석 서버 연동
- 이미지 분석 결과 저장
- 농업 문서 업로드 / 파싱 / chunking / embedding / pgvector 저장
- RAG 검색
- 분석 리포트 생성
- SSE 진행 상태
- 외부 API Client 구조
- MCP Client 구조

## 3. Spring Boot 코드 스타일

- Java 17을 사용합니다.
- Spring Boot 3.x를 사용합니다.
- API 경로는 `/api/v1` prefix를 표준으로 사용합니다.
- 패키지는 domain 기준으로 분리합니다.
- Controller / Service / Repository / DTO / Entity를 분리합니다.
- Entity를 직접 Response로 반환하지 않습니다.
- 모든 API 응답은 공통 응답 포맷 `ApiResponse<T>`를 사용합니다.
- 예외는 `GlobalExceptionHandler`에서 처리합니다.
- Lombok 사용 가능합니다.
- Validation을 사용합니다.
- Swagger/OpenAPI를 사용합니다.
- JWT 인증을 사용합니다.
- 테스트 코드는 가능한 범위에서 작성합니다.

## 4. FastAPI 코드 스타일

- Python 3.11 이상을 사용합니다.
- FastAPI를 사용합니다.
- 병충해 분석 API는 `/api/v1/disease/predict`로 제공합니다.
- 초기에는 더미 예측 결과를 반환합니다.
- 나중에 ML 모델을 교체할 수 있도록 `DiseasePredictor` 인터페이스 또는 서비스 클래스를 분리합니다.
- 응답 스키마는 Pydantic 모델을 사용합니다.

## 5. DB 규칙

- PostgreSQL을 사용합니다.
- pgvector 확장을 사용합니다.
- 일반 서비스 데이터와 vector 데이터를 같은 PostgreSQL에 저장합니다.
- 마이그레이션 관리는 Flyway를 사용합니다.
- 테이블명은 snake_case를 사용합니다.
- `created_at`, `updated_at` 공통 필드를 사용합니다.

## 6. 금지 사항

- 프론트엔드 구현 금지
- FastAPI 서버가 직접 PostgreSQL에 접근하지 않게 할 것
- FastAPI 서버는 이미지 분석 결과만 반환할 것
- DB 저장은 Spring Boot가 담당할 것
- API Key나 Secret을 코드에 하드코딩하지 말 것
- 너무 큰 기능을 한 번에 구현하지 말 것

## 7. 완료 기준

- `docker compose up`으로 PostgreSQL, Spring Boot, FastAPI가 실행 가능해야 합니다.
- Spring Boot Swagger 페이지에서 주요 API 확인 가능해야 합니다.
- Spring Boot에서 FastAPI `/api/v1/disease/predict`를 호출할 수 있어야 합니다.
- 이미지 업로드 후 더미 병충해 분석 결과가 DB에 저장되어야 합니다.
- RAG 문서 chunk 저장 구조가 있어야 합니다.
- SSE 샘플 API가 있어야 합니다.

## 8. 추가 작업 원칙

- Spring Boot가 오케스트레이션과 데이터 저장을 담당합니다.
- FastAPI는 분석 전용 서버로 유지하고, 상태 저장 책임을 갖지 않습니다.
- RAG 파이프라인은 Spring Boot 중심으로 구성합니다.
- 환경 변수는 `.env` 기반 설정을 우선합니다.
- 구조를 먼저 명확히 만들고, 복잡한 기능은 작은 단위로 점진적으로 확장합니다.
