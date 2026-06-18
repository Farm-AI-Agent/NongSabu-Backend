from __future__ import annotations

import logging
import math
import os
import re
from contextlib import asynccontextmanager
from functools import lru_cache
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


DEFAULT_MODEL = "BAAI/bge-reranker-v2-m3"
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger("nongsabu.reranker")
startup_error: str | None = None


class RerankCandidate(BaseModel):
    chunk_id: str = Field(..., min_length=1)
    text: str = ""
    metadata: dict[str, Any] = Field(default_factory=dict)


class RerankRequest(BaseModel):
    query: str = Field(..., min_length=1)
    top_n: int = Field(default=4, ge=1, le=100)
    candidates: list[RerankCandidate] = Field(default_factory=list)


class RerankResult(BaseModel):
    chunk_id: str
    rerank_rank: int
    rerank_score: float


class RerankResponse(BaseModel):
    model: str
    backend: str
    fallback_used: bool
    results: list[RerankResult]


@asynccontextmanager
async def lifespan(app: FastAPI):
    global startup_error
    if load_on_startup():
        try:
            logger.info("Loading reranker model on startup: %s", model_name())
            load_cross_encoder()
            startup_error = None
            logger.info("Reranker model is ready: %s", model_name())
        except Exception as exc:
            startup_error = f"{type(exc).__name__}: {exc}"
            logger.exception("Failed to load reranker model on startup")
            if not fallback_allowed():
                raise
    yield


app = FastAPI(
    title="NongSabu Reranker Server",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
def health() -> dict[str, Any]:
    model_loaded = load_cross_encoder.cache_info().currsize > 0 and startup_error is None
    if model_loaded:
        status = "ready"
        backend = "sentence-transformers"
    elif startup_error is not None and fallback_allowed():
        status = "degraded"
        backend = "lexical-fallback"
    elif load_on_startup():
        status = "not-ready"
        backend = "unavailable"
    else:
        status = "lazy"
        backend = "sentence-transformers"

    return {
        "status": status,
        "model": model_name(),
        "backend": backend,
        "model_loaded": model_loaded,
        "load_on_startup": load_on_startup(),
        "fallback_allowed": fallback_allowed(),
        "startup_error": startup_error,
    }


@app.post("/api/v1/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest) -> RerankResponse:
    if not request.candidates:
        return RerankResponse(
            model=model_name(),
            backend="empty",
            fallback_used=False,
            results=[],
        )

    top_n = min(request.top_n, len(request.candidates))
    try:
        if startup_error is not None and load_on_startup():
            raise RuntimeError(startup_error)
        scores = model_scores(request.query, request.candidates)
        backend = "sentence-transformers"
        fallback_used = False
    except Exception as exc:
        if not fallback_allowed():
            raise HTTPException(
                status_code=503,
                detail=f"Reranker model is not available: {exc}",
            ) from exc
        logger.warning("Using lexical fallback reranker: %s", exc)
        scores = lexical_scores(request.query, request.candidates)
        backend = "lexical-fallback"
        fallback_used = True

    ranked = sorted(
        zip(request.candidates, scores, strict=True),
        key=lambda pair: pair[1],
        reverse=True,
    )[:top_n]

    return RerankResponse(
        model=model_name(),
        backend=backend,
        fallback_used=fallback_used,
        results=[
            RerankResult(
                chunk_id=candidate.chunk_id,
                rerank_rank=index + 1,
                rerank_score=float(score),
            )
            for index, (candidate, score) in enumerate(ranked)
        ],
    )


def model_scores(query: str, candidates: list[RerankCandidate]) -> list[float]:
    model = load_cross_encoder()
    pairs = [[query, candidate.text] for candidate in candidates]
    raw_scores = model.predict(pairs)
    return [float(score) for score in raw_scores]


@lru_cache(maxsize=1)
def load_cross_encoder():
    from sentence_transformers import CrossEncoder

    return CrossEncoder(
        model_name(),
        max_length=int(os.getenv("RERANKER_MAX_LENGTH", "512")),
    )


def model_name() -> str:
    return os.getenv("RERANKER_MODEL", DEFAULT_MODEL)


def load_on_startup() -> bool:
    return os.getenv("RERANKER_LOAD_ON_STARTUP", "true").lower() in {"1", "true", "yes", "on"}


def fallback_allowed() -> bool:
    return os.getenv("RERANKER_ALLOW_FALLBACK", "true").lower() in {"1", "true", "yes", "on"}


def lexical_scores(query: str, candidates: list[RerankCandidate]) -> list[float]:
    query_terms = tokenize(query)
    if not query_terms:
        return [0.0 for _ in candidates]

    scores: list[float] = []
    for candidate in candidates:
        text_terms = tokenize(candidate.text)
        metadata_terms = tokenize(" ".join(str(value) for value in candidate.metadata.values()))
        all_terms = text_terms + metadata_terms
        if not all_terms:
            scores.append(0.0)
            continue

        overlap = sum(1 for term in query_terms if term in all_terms)
        coverage = overlap / len(query_terms)
        density = overlap / math.sqrt(len(all_terms))
        scores.append(float(coverage + density))
    return scores


def tokenize(text: str) -> list[str]:
    return [
        token.lower()
        for token in re.findall(r"[0-9A-Za-z]+|[가-힣]+", text)
        if len(token) >= 2
    ]
