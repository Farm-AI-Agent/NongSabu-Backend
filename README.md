# NongSabu Backend

This repository contains the backend foundation for a farm AI assistant focused on beginner and small-scale farmers. The current scope is backend-only: Spring Boot as the main API server, FastAPI as the image disease analysis server, and PostgreSQL with pgvector as the shared data store.

## Root Structure

```text
project-root/
├── backend-spring/
├── ai-server/
├── docker-compose.yml
├── .env.example
├── README.md
└── AGENTS.md
```

## Stack

- `backend-spring`: Spring Boot 3.x, Java 17, Gradle
- `ai-server`: FastAPI, Python 3.11+
- `postgres`: PostgreSQL 16 with pgvector

## Local Run

1. Copy `.env.example` to `.env`.
2. Run the full stack from the project root.

```bash
docker-compose up --build
```

## Health Checks

- Spring Boot: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- FastAPI: [http://localhost:8000/health](http://localhost:8000/health)

## Useful Local Endpoints

- Spring Boot Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- FastAPI Docs: [http://localhost:8000/docs](http://localhost:8000/docs)

## What Is Included

- Spring Boot project structure with domain-oriented packages
- Security, validation, JPA, PostgreSQL, Flyway, OpenAPI, WebFlux dependencies
- FastAPI server with `/api/v1/disease/predict`
- Docker Compose setup for `postgres`, `ai-server`, and `backend-spring`
- pgvector extension bootstrap via Flyway migration
- Base structure for image analysis, RAG, SSE, external API clients, and MCP clients

## Notes

- The FastAPI server currently returns a dummy disease prediction result.
- The Spring Boot server is prepared to call the FastAPI prediction endpoint.
- Spring AI integration is intentionally left as a future step, with a placeholder structure to avoid unstable version conflicts during initial setup.
