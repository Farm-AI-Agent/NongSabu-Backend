"""추론기 정의.

현재 '실행되는' 코드는 사전정의 결과를 돌려주는 목업(GrapeMockDiseasePredictor)이다.
실제 YOLO26 ONNX 추론 코드는 이 파일 하단에 구현되어 있으나 **주석 처리**되어 있다.
모델 학습/변환 완료 후 주석을 해제하고 `uv sync --group inference` 를 실행하면 동작한다.
"""

from __future__ import annotations

from io import BytesIO
from typing import Protocol

from fastapi import UploadFile
from PIL import Image, UnidentifiedImageError

from app.core.config import Settings
from app.core.errors import InvalidImageError
from app.schemas.analysis import AnalysisResponse


class DiseasePredictor(Protocol):
    def predict_sync(self, file_bytes: bytes, filename: str | None) -> AnalysisResponse: ...


def _image_size(file_bytes: bytes) -> tuple[int, int]:
    """이미지 (너비, 높이) 반환. 디코딩 불가 시 InvalidImageError(목업도 입력 검증)."""
    if not file_bytes:
        raise InvalidImageError("빈 파일입니다.")
    try:
        with Image.open(BytesIO(file_bytes)) as image:
            return image.size  # (width, height)
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise InvalidImageError("이미지를 디코딩할 수 없습니다.") from exc


class GrapeMockDiseasePredictor:
    """실행용 목업 추론기 — dev 더미와 동일한 사전정의 결과를 반환한다.

    모델은 '포도 전용'이며, 포도 외 작물 차단(작물 게이팅)은 Spring 이 ai-server 호출 전에 처리한다.
    실제 ONNX 모델 연결 전까지, Spring 으로 그대로 전달 가능한 7필드 형태의 고정 결과를 돌려준다.
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def predict_sync(self, file_bytes: bytes, filename: str | None = None) -> AnalysisResponse:
        width, height = _image_size(file_bytes)  # 잘못된 이미지면 InvalidImageError
        return AnalysisResponse(
            diagnosis="Grape disease suspicion (dummy)",
            confidence=0.87,
            severity="LOW",
            summary=(
                "This MVP dummy predictor represents the grape disease analysis flow. "
                "A real grape ML model can replace GrapeMockDiseasePredictor later."
            ),
            recommended_action=(
                "Inspect grape leaves and clusters, isolate suspicious vines if needed, "
                "and review grape disease guidance before applying treatment."
            ),
            model_version=f"grape-dummy-disease-predictor-v1:{len(file_bytes)}:{width}x{height}",
        )

    def info(self) -> dict:
        return {
            "mode": "mock",
            "model_loaded": False,
            "model_version": "grape-dummy-disease-predictor-v1",
        }

    async def predict(self, file: UploadFile) -> AnalysisResponse:
        return self.predict_sync(await file.read(), file.filename)


def build_predictor(settings: Settings) -> GrapeMockDiseasePredictor:
    """현재는 목업을 반환. 실모델 연결 시 아래 ONNX 경로(주석)로 교체한다."""
    return GrapeMockDiseasePredictor(settings)


# =============================================================================
# 실제 YOLO26 ONNX 추론 — 모델 학습/변환 완료 후 주석 해제.
#
# 활성화 절차:
#   1) uv sync --group inference          # numpy, onnxruntime 설치
#   2) 아래 임포트/클래스 주석 해제
#   3) 위 build_predictor 를 아래 ONNX 버전으로 교체(또는 분기)
#   4) AI_MODEL_PATH 로 .onnx 경로 지정, labels.json(클래스 순서) 작성
#   5) 응답은 동일한 7필드 AnalysisResponse 로 매핑(아래 _aggregate 참고)
#
# import json
# from pathlib import Path
#
# import numpy as np
#
# from app.services.onnx_engine import OnnxYoloEngine
#
# _SEVERITY_RANK = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
#
#
# def _load_labels(settings: Settings) -> list[dict]:
#     if not settings.labels_path or not Path(settings.labels_path).is_file():
#         return []
#     return json.loads(Path(settings.labels_path).read_text(encoding="utf-8")).get("classes", [])
#
#
# def _decode_image_array(file_bytes: bytes) -> "np.ndarray":
#     if not file_bytes:
#         raise InvalidImageError("빈 파일입니다.")
#     try:
#         with Image.open(BytesIO(file_bytes)) as image:
#             return np.asarray(image.convert("RGB"))
#     except (UnidentifiedImageError, OSError, ValueError) as exc:
#         raise InvalidImageError("이미지를 디코딩할 수 없습니다.") from exc
#
#
# class OnnxDiseasePredictor:
#     """ONNX 엔진으로 추론. 무거운 추론은 라우터에서 스레드풀로 위임(동시성 제한)."""
#
#     def __init__(self, settings: Settings, engine) -> None:
#         self._settings = settings
#         self._engine = engine
#         self._labels = _load_labels(settings)
#
#     def _label(self, class_id: int) -> tuple[str, str]:
#         if 0 <= class_id < len(self._labels):
#             c = self._labels[class_id]
#             return c.get("name_ko", f"클래스 {class_id}"), c.get("severity", "LOW")
#         return f"클래스 {class_id}", "LOW"
#
#     def predict_sync(self, file_bytes: bytes, filename: str | None = None) -> AnalysisResponse:
#         image = _decode_image_array(file_bytes)
#         detections = []  # [(label_ko, confidence, severity), ...]
#         for class_id, conf, _bbox in self._engine.infer(image):
#             label_ko, severity = self._label(class_id)
#             detections.append((label_ko, conf, severity))
#         detections.sort(key=lambda d: d[1], reverse=True)
#         return self._aggregate(detections)
#
#     def _aggregate(self, detections) -> AnalysisResponse:
#         # 탐지 0건 = 정상 (별도 클래스 아님)
#         if not detections:
#             return AnalysisResponse(
#                 diagnosis="정상", confidence=0.0, severity="LOW",
#                 summary="포도 잎에서 병충해가 탐지되지 않았습니다(정상).",
#                 recommended_action="", model_version=self._settings.model_version,
#             )
#         top = detections[0]
#         worst = max(detections, key=lambda d: _SEVERITY_RANK.get(d[2], 0))
#         return AnalysisResponse(
#             diagnosis=top[0], confidence=top[1], severity=worst[2],
#             summary=f"{len(detections)}건의 병충해가 탐지되었습니다(대표: {top[0]}).",
#             recommended_action="",  # 대응문구는 Spring RAG 가 생성(팀 합의)
#             model_version=self._settings.model_version,
#         )
#
#     def info(self) -> dict:
#         return {"mode": "onnx", "model_loaded": True, "model_version": self._settings.model_version}
#
#     async def predict(self, file: UploadFile) -> AnalysisResponse:
#         return self.predict_sync(await file.read(), file.filename)
#
#
# def build_predictor(settings: Settings):
#     model_path = settings.resolved_model_path()
#     if model_path is None:
#         return GrapeMockDiseasePredictor(settings)   # 모델 없으면 목업 폴백
#     engine = OnnxYoloEngine(str(model_path), settings)
#     return OnnxDiseasePredictor(settings, engine)
# =============================================================================
