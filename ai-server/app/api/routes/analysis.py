import anyio
from fastapi import APIRouter, File, Request, UploadFile

from app.core.errors import InferenceError, InvalidImageError, PayloadTooLargeError
from app.schemas.analysis import AnalysisResponse

router = APIRouter()


@router.post("/disease/predict", response_model=AnalysisResponse)
async def predict_disease(request: Request, file: UploadFile = File(...)) -> AnalysisResponse:
    settings = request.app.state.settings
    predictor = request.app.state.predictor
    limiter = request.app.state.inference_limiter

    # 입력 검증 ──────────────────────────────────────────────
    content_type = (file.content_type or "").lower()
    if not content_type.startswith("image/"):
        raise InvalidImageError(f"이미지 파일이 아닙니다(content-type={content_type or 'unknown'}).")

    file_bytes = await file.read()
    if not file_bytes:
        raise InvalidImageError("빈 파일입니다.")
    if len(file_bytes) > settings.max_upload_bytes:
        raise PayloadTooLargeError(f"파일이 너무 큽니다(최대 {settings.max_upload_mb}MB).")

    # 추론은 CPU 바운드 → 스레드풀 + capacity limiter 로 동시 실행 수 제한(CPU 보호).
    try:
        result = await anyio.to_thread.run_sync(
            predictor.predict_sync, file_bytes, file.filename, limiter=limiter
        )
    except InvalidImageError:
        raise  # 이미지 디코딩 실패는 그대로 400
    except Exception as exc:  # 그 외 추론 중 오류 → 500 INFERENCE_FAILED
        raise InferenceError(f"추론에 실패했습니다: {exc}") from exc

    # 검증용: 입력 이미지 + bbox 그린 결과를 디버그 폴더에 저장(API 응답엔 영향 없음).
    if settings.save_annotated:
        from app.services.annotate import save_annotated

        await anyio.to_thread.run_sync(
            save_annotated, file_bytes, result.detections, settings.annotated_dir, file.filename
        )

    return result
