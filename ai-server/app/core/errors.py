"""도메인 예외 + FastAPI 예외 핸들러. 응답은 docs/API.md §5 의 ErrorResponse 계약을 따른다."""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.core.config import get_settings
from app.schemas.analysis import ErrorResponse

logger = logging.getLogger("ai-server")


class AiServerError(Exception):
    status_code = 500
    error_code = "INTERNAL_ERROR"

    def __init__(self, message: str) -> None:
        super().__init__(message)
        self.message = message


class InvalidImageError(AiServerError):
    status_code = 400
    error_code = "INVALID_IMAGE"


class PayloadTooLargeError(AiServerError):
    status_code = 413
    error_code = "PAYLOAD_TOO_LARGE"


class InferenceError(AiServerError):
    status_code = 500
    error_code = "INFERENCE_FAILED"


def _body(error_code: str, message: str) -> dict:
    return ErrorResponse(
        error_code=error_code, message=message, model_version=get_settings().model_version
    ).model_dump()


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AiServerError)
    async def _handle_known(request: Request, exc: AiServerError) -> JSONResponse:
        if exc.status_code >= 500:
            logger.exception("%s: %s", exc.error_code, exc.message)
        else:
            logger.warning("%s: %s", exc.error_code, exc.message)
        return JSONResponse(status_code=exc.status_code, content=_body(exc.error_code, exc.message))

    @app.exception_handler(Exception)
    async def _handle_unexpected(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled error")
        return JSONResponse(status_code=500, content=_body("INTERNAL_ERROR", "내부 오류가 발생했습니다."))
