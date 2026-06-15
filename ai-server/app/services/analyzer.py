from fastapi import UploadFile

from app.schemas.analysis import AnalysisResponse


class CropDiseaseAnalyzer:
    async def analyze(self, file: UploadFile) -> AnalysisResponse:
        await file.read()
        filename = (file.filename or "").lower()

        if "tomato" in filename or "토마토" in filename:
            diagnosis = "토마토 잎마름병 의심"
            severity = "MEDIUM"
        elif "strawberry" in filename or "딸기" in filename:
            diagnosis = "딸기 흰가루병 의심"
            severity = "HIGH"
        else:
            diagnosis = "초기 병반 의심"
            severity = "LOW"

        return AnalysisResponse(
            diagnosis=diagnosis,
            confidence=0.87,
            severity=severity,
            summary="현재 AI 서버는 더미 분석 결과를 반환합니다. 이후 PyTorch 기반 실제 모델로 교체할 수 있습니다.",
            recommended_action="의심 부위를 격리 관찰하고, 재배 매뉴얼 및 등록 약제를 확인한 뒤 초기 방제 여부를 판단하세요.",
            model_version="dummy-analyzer-v1",
        )

