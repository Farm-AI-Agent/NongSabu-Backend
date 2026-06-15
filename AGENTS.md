# AGENTS

## 1. 프로젝트 개요
- 이 프로젝트는 초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스다.
- Spring Boot 서버가 메인 백엔드 역할을 한다.
- FastAPI 서버는 작물 이미지 병충해 분석만 담당한다.
- PostgreSQL + pgvector는 서비스 데이터와 RAG 벡터 저장소로 사용한다.
- 프론트엔드는 현재 구현하지 않는다.

## 2. 구현 범위

### 포함
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
- Java 17 사용
- Spring Boot 3.x 사용
- 패키지는 domain 기준으로 분리한다
- Controller / Service / Repository / DTO / Entity 분리
- Entity를 직접 Response로 반환하지 않는다
- 모든 API 응답은 공통 응답 포맷 `ApiResponse<T>`를 사용한다
- 예외는 `GlobalExceptionHandler`에서 처리한다
- Lombok 사용 가능
- Validation 사용
- Swagger/OpenAPI 사용
- JWT 인증 사용
- 테스트 코드는 가능한 범위에서 작성한다

## 4. FastAPI 코드 스타일
- Python 3.11 이상 사용
- FastAPI 사용
- 병충해 분석 API는 `/api/v1/disease/predict` 로 제공한다
- 초기에는 더미 예측 결과를 반환한다
- 나중에 ML 모델을 교체할 수 있도록 `DiseasePredictor` 인터페이스 또는 서비스 클래스를 분리한다
- 응답 스키마는 Pydantic 모델을 사용한다

## 5. DB 규칙
- PostgreSQL 사용
- pgvector 확장 사용
- 일반 서비스 데이터와 vector 데이터를 같은 PostgreSQL에 저장한다
- Flyway 또는 Liquibase 중 하나로 마이그레이션 관리한다
- 우선 Flyway를 사용한다
- 테이블명은 snake_case 사용
- `created_at`, `updated_at` 공통 필드를 사용한다

## 6. 금지 사항
- 프론트엔드 구현 금지
- FastAPI 서버가 직접 PostgreSQL에 접근하지 않게 할 것
- FastAPI 서버는 이미지 분석 결과만 반환한다
- DB 저장은 Spring Boot가 담당한다
- API Key나 Secret을 코드에 하드코딩하지 말 것
- 너무 큰 기능을 한 번에 구현하지 말 것

## 7. 완료 기준
- `docker-compose up` 으로 PostgreSQL, Spring Boot, FastAPI가 실행 가능해야 한다
- Spring Boot Swagger 페이지에서 주요 API 확인 가능해야 한다
- Spring Boot에서 FastAPI `/api/v1/disease/predict` 를 호출할 수 있어야 한다
- 이미지 업로드 후 더미 병충해 분석 결과가 DB에 저장되어야 한다
- RAG 문서 chunk 저장 구조가 있어야 한다
- SSE 샘플 API가 있어야 한다

## 추가 작업 원칙
- Spring Boot가 오케스트레이션과 데이터 저장을 담당한다.
- FastAPI는 분석 전용 서버로 유지하고, 상태 저장 책임을 갖지 않는다.
- RAG 파이프라인은 Spring Boot 중심으로 구성한다.
- 환경 변수와 `.env` 기반 설정을 우선한다.
- 구조를 먼저 명확히 만들고, 복잡한 기능은 작은 단위로 점진적으로 확장한다.
