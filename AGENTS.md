# AGENTS

## 목적
- 이 저장소는 초보·소규모 농가를 위한 맞춤형 농업 AI 비서 서비스의 백엔드 MVP를 다룬다.
- 프론트엔드는 제외하고 Spring Boot, FastAPI, PostgreSQL + pgvector 중심으로 개발한다.

## 기본 원칙
- Spring Boot가 메인 오케스트레이션 서버다.
- FastAPI는 병충해 이미지 분석 전용 서버다.
- RAG 파이프라인은 Spring Boot에서 관리한다.
- 모든 민감 정보는 환경 변수 또는 `.env`로 주입한다.
- 로컬 개발은 `docker compose up --build` 기준으로 유지한다.

## 코드 규칙
- Spring Boot는 `controller -> service -> repository -> entity/dto` 계층을 분리한다.
- FastAPI는 `api -> schemas -> services -> models` 구조를 유지한다.
- 공통 응답 포맷과 전역 예외 처리를 사용한다.
- 새로운 외부 연동은 `infra` 하위에 client 인터페이스와 구현체를 둔다.
- AI/LLM/Embedding 구현은 교체 가능하도록 인터페이스 중심으로 만든다.

## 확장 가이드
- 실제 병충해 모델 도입 시 `ai-server/app/services/analyzer.py`만 교체 가능해야 한다.
- 실제 임베딩 모델 도입 시 Spring Boot의 embedding 서비스 구현만 교체 가능해야 한다.
- MCP 및 Tool Calling 연동은 `infra/mcp` 패키지를 확장해 붙인다.

