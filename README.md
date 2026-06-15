# NongSabu Backend

초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스의 백엔드 저장소다. 현재 단계에서는 프론트엔드 없이 Spring Boot API 서버, FastAPI 이미지 분석 서버, PostgreSQL + pgvector 기반의 백엔드 MVP를 구현한다.

## 루트 구조

```text
project-root/
├── backend-spring/
├── ai-server/
├── docker-compose.yml
├── .env.example
├── README.md
└── AGENTS.md
```

## 기술 스택

- `backend-spring`: Spring Boot 3.x, Java 17, Gradle
- `ai-server`: FastAPI, Python 3.11+
- `postgres`: PostgreSQL 16 + pgvector

## 로컬 실행 방법

1. `.env.example`을 복사해서 `.env` 파일을 만든다.
2. 프로젝트 루트에서 아래 명령을 실행한다.

```bash
docker-compose up --build
```

## 헬스 체크

- Spring Boot: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- FastAPI: [http://localhost:8000/health](http://localhost:8000/health)

## 주요 확인 주소

- Spring Boot Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- FastAPI Docs: [http://localhost:8000/docs](http://localhost:8000/docs)

## 현재 포함된 기본 구조

- 도메인 기준으로 분리된 Spring Boot 패키지 구조
- Spring Security, Validation, JPA, PostgreSQL, Flyway, OpenAPI, WebFlux 의존성
- FastAPI `/api/v1/disease/predict` 더미 병충해 예측 API
- `postgres`, `ai-server`, `backend-spring`을 포함한 Docker Compose 구성
- pgvector 확장 활성화 및 핵심 도메인 스키마용 Flyway 마이그레이션
- 이미지 분석, RAG, SSE, 외부 API, MCP 연동을 위한 기본 확장 구조

## 참고

- FastAPI 서버는 현재 더미 병충해 분석 결과를 반환한다.
- Spring Boot 서버는 FastAPI 예측 엔드포인트를 호출할 수 있는 구조를 갖춘 상태다.
- Spring AI 연동은 버전 충돌 가능성을 피하기 위해 구조만 준비하고 실제 연동은 추후 단계에서 진행한다.
