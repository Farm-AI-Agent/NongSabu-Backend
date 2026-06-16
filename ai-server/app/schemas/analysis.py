from pydantic import BaseModel, ConfigDict, Field


class AnalysisResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    success: bool = True
    diagnosis: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    severity: str
    summary: str
    recommended_action: str
    model_version: str
