from io import BytesIO
from typing import Protocol

from fastapi import UploadFile
from PIL import Image

from app.schemas.analysis import AnalysisResponse


class DiseasePredictor(Protocol):
    async def predict(self, file: UploadFile) -> AnalysisResponse:
        ...


class GrapeDummyDiseasePredictor:
    """MVP-only grape disease predictor stub.

    Spring Boot decides whether a crop is supported before calling FastAPI.
    This service therefore represents only the grape analysis path and can be
    replaced by a real grape ML model without changing the API route.
    """

    async def predict(self, file: UploadFile) -> AnalysisResponse:
        file_bytes = await file.read()

        try:
            with Image.open(BytesIO(file_bytes)) as image:
                width, height = image.size
        except Exception:
            width, height = 0, 0

        return AnalysisResponse(
            diagnosis="Grape disease suspicion (dummy)",
            confidence=0.87,
            severity="LOW",
            summary=(
                "This MVP dummy predictor represents the grape disease analysis flow. "
                "A real grape ML model can replace GrapeDummyDiseasePredictor later."
            ),
            recommended_action=(
                "Inspect grape leaves and clusters, isolate suspicious vines if needed, "
                "and review grape disease guidance before applying treatment."
            ),
            model_version=f"grape-dummy-disease-predictor-v1:{len(file_bytes)}:{width}x{height}",
        )
