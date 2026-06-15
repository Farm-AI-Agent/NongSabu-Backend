from pydantic import BaseModel, Field


class AnalysisResponse(BaseModel):
    success: bool = True
    diagnosis: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    severity: str
    summary: str
    recommended_action: str
    model_version: str

