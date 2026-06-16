from fastapi import APIRouter, File, UploadFile

from app.schemas.analysis import AnalysisResponse
from app.services.disease_predictor import GrapeDummyDiseasePredictor


router = APIRouter()
disease_predictor = GrapeDummyDiseasePredictor()


@router.post("/disease/predict", response_model=AnalysisResponse)
async def predict_disease(file: UploadFile = File(...)) -> AnalysisResponse:
    return await disease_predictor.predict(file)
