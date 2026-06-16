from pydantic import BaseModel, Field


class Detection(BaseModel):
    """YOLO 탐지 1건. bbox 는 원본 이미지 좌표계의 [x, y, width, height] (픽셀)."""

    class_name: str
    label_ko: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    severity: str
    bbox: list[float] = Field(..., min_length=4, max_length=4)


class AnalysisResponse(BaseModel):
    # ── Spring AiAnalysisResponse 계약(요약 필드) — 필드명/타입 변경 금지 ──
    success: bool = True
    diagnosis: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    severity: str
    summary: str
    recommended_action: str = ""
    model_version: str

    # ── YOLO 상세(확장) — Spring record 는 모르는 필드를 무시하므로 하위호환 ──
    detections: list[Detection] = Field(default_factory=list)
    detection_count: int = 0
    image_size: dict[str, int] = Field(default_factory=dict)
    inference_time_ms: float = 0.0


class ErrorResponse(BaseModel):
    success: bool = False
    error_code: str
    message: str
    model_version: str
