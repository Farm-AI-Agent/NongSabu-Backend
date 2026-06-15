from fastapi import APIRouter, File, UploadFile

from app.schemas.analysis import AnalysisResponse
from app.services.analyzer import CropDiseaseAnalyzer


router = APIRouter()
analyzer = CropDiseaseAnalyzer()


@router.post("/analyze", response_model=AnalysisResponse)
async def analyze(file: UploadFile = File(...)) -> AnalysisResponse:
    return await analyzer.analyze(file)

