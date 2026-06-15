from io import BytesIO
from typing import Protocol

from fastapi import UploadFile
from PIL import Image

from app.schemas.analysis import AnalysisResponse


class DiseasePredictor(Protocol):
    async def predict(self, file: UploadFile) -> AnalysisResponse:
        ...


class DummyDiseasePredictor:
    async def predict(self, file: UploadFile) -> AnalysisResponse:
        file_bytes = await file.read()
        filename = (file.filename or "").lower()

        try:
            with Image.open(BytesIO(file_bytes)) as image:
                width, height = image.size
        except Exception:
            width, height = 0, 0

        if "tomato" in filename:
            diagnosis = "Tomato early blight suspicion"
            severity = "MEDIUM"
        elif "strawberry" in filename:
            diagnosis = "Strawberry powdery mildew suspicion"
            severity = "HIGH"
        else:
            diagnosis = "General crop disease suspicion"
            severity = "LOW"

        return AnalysisResponse(
            diagnosis=diagnosis,
            confidence=0.87,
            severity=severity,
            summary=(
                "This server currently returns a dummy prediction result. "
                "A real ML model can replace DummyDiseasePredictor later."
            ),
            recommended_action=(
                "Inspect the affected area, isolate suspicious crops if needed, "
                "and review the cultivation manual before applying treatment."
            ),
            model_version=f"dummy-disease-predictor-v1:{len(file_bytes)}:{width}x{height}",
        )
