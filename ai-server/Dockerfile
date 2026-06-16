# 베이스 OS 는 미확정 — 아래 첫 줄만 교체하면 됨(예: nvidia/cuda 베이스).
# alpine 은 (ONNX 활성화 시) 휠 호환성 문제로 피하고 Debian slim 권장.
FROM python:3.13-slim AS base

# uv 바이너리만 멀티스테이지로 복사(이미지에 uv 설치 불필요)
COPY --from=ghcr.io/astral-sh/uv:latest /uv /bin/uv

ENV UV_LINK_MODE=copy \
    UV_COMPILE_BYTECODE=1 \
    UV_PYTHON_DOWNLOADS=never \
    PATH="/app/.venv/bin:$PATH"

WORKDIR /app

# 1) 의존성만 먼저 설치(레이어 캐시). 기본 그룹만 → 목업 실행용 슬림 런타임.
#    (ONNX 활성화 시: --group inference 추가)
COPY pyproject.toml uv.lock ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-install-project --no-dev

# 2) 소스 복사 후 프로젝트 설치
COPY app ./app
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
