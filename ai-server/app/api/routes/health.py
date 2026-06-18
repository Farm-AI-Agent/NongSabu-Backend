from fastapi import APIRouter, Request

router = APIRouter()


@router.get("/health")
def health(request: Request) -> dict:
    """Liveness + 모델 적재 여부(간단). 도커 healthcheck 용."""
    predictor = getattr(request.app.state, "predictor", None)
    info = predictor.info() if predictor is not None else {}
    return {"status": "ok", "model_loaded": info.get("model_loaded", False), "mode": info.get("mode")}


@router.get("/info")
def info(request: Request) -> dict:
    """현재 적재된 추론기/모델 메타데이터(버전, 클래스, 추론 파라미터)."""
    predictor = getattr(request.app.state, "predictor", None)
    if predictor is None:
        return {"model_loaded": False}
    return predictor.info()
