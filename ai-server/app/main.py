import logging
import time
from contextlib import asynccontextmanager

import anyio
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from app.api.router import api_router
from app.core.config import get_settings
from app.core.errors import register_exception_handlers
from app.services.disease_predictor import build_predictor

logger = logging.getLogger("ai-server")


def _configure_logging() -> None:
    if not logger.handlers:
        handler = logging.StreamHandler()
        handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s [%(name)s] %(message)s"))
        logger.addHandler(handler)
    logger.setLevel(logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    _configure_logging()
    app.state.settings = settings
    # 모델은 기동 시 1회 로드해 app.state 에 보관(요청마다 재로딩 금지).
    app.state.predictor = build_predictor(settings)
    # 동시 추론 수 제한(CPU 보호). to_thread.run_sync 가 이 limiter 를 공유한다.
    app.state.inference_limiter = anyio.CapacityLimiter(settings.resolved_max_concurrency())
    logger.info(
        "ai-server 기동: mode=%s, model_loaded=%s, max_concurrency=%s",
        app.state.predictor.info().get("mode"),
        app.state.predictor.info().get("model_loaded"),
        settings.resolved_max_concurrency(),
    )
    yield


app = FastAPI(
    title="NongSabu AI Server",
    description="Crop disease prediction server for NongSabu backend",
    version="0.1.0",
    lifespan=lifespan,
)

_settings = get_settings()
if _settings.cors_origins_list():
    app.add_middleware(
        CORSMiddleware,
        allow_origins=_settings.cors_origins_list(),
        allow_methods=["*"],
        allow_headers=["*"],
    )

register_exception_handlers(app)
app.include_router(api_router)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    started = time.perf_counter()
    response = await call_next(request)
    elapsed_ms = (time.perf_counter() - started) * 1000
    logger.info("%s %s -> %s (%.1fms)", request.method, request.url.path, response.status_code, elapsed_ms)
    return response


@app.get("/", tags=["root"])
def root() -> dict[str, str]:
    return {"message": "NongSabu AI Server is running"}
