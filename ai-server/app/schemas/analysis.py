from pydantic import BaseModel, ConfigDict, Field


class AnalysisResponse(BaseModel):
    # dev 와 동일한 계약(7필드, snake_case). Spring AiAnalysisResponse 와 1:1 매핑.
    # protected_namespaces=() : "model_version" 필드의 pydantic 경고 억제(dev 기준).
    model_config = ConfigDict(protected_namespaces=())

    success: bool = True
    diagnosis: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    severity: str
    summary: str
    recommended_action: str
    model_version: str


class ErrorResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    success: bool = False
    error_code: str
    message: str
    model_version: str
