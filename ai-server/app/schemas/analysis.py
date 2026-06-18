from pydantic import BaseModel, ConfigDict, Field


class Detection(BaseModel):
    """탐지 1건 — 클라이언트가 원본 이미지 위에 박스를 그리는 데 필요한 정보.

    bbox 는 **분석한 원본 이미지 좌표계**의 [x, y, width, height] (픽셀, 좌상단 원점).
    클라이언트는 image_size 를 기준으로 자신이 표시하는 크기에 맞게 비율 변환해 그린다.
    """

    class_name: str  # 영문 클래스 id (예: "downy_mildew")
    label: str       # 표시용 한글명 (예: "노균병")
    confidence: float = Field(..., ge=0.0, le=1.0)
    confidence_percent: float = Field(0.0, ge=0.0, le=100.0)
    bbox: list[float] = Field(..., min_length=4, max_length=4)


class AnalysisResponse(BaseModel):
    # dev 와 동일한 7필드(snake_case) + protected_namespaces. Spring AiAnalysisResponse 와 1:1.
    model_config = ConfigDict(protected_namespaces=())

    success: bool = True
    diagnosis: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    severity: str
    summary: str
    recommended_action: str
    model_version: str

    # ── 클라이언트 렌더링용 확장 ── (Spring record 는 모르는 필드를 무시 → 하위호환)
    # 결과 이미지를 보내지 않고 좌표만 전달 → 클라이언트가 자신의 원본에 박스를 그린다.
    detections: list[Detection] = Field(default_factory=list)
    detection_count: int = 0
    image_size: dict[str, int] = Field(default_factory=dict)  # 좌표 기준 원본 해상도 {width,height}


class ErrorResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    success: bool = False
    error_code: str
    message: str
    model_version: str
