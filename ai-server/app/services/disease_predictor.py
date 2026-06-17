"""추론기 정의.

현재 '실행되는' 코드는 실제 YOLO26 ONNX 추론기(OnnxDiseasePredictor)다.
목업(GrapeMockDiseasePredictor)은 이 파일 하단에 **주석 처리**로 보존되어 있다
(모델/추론 의존성 없이 통신만 검증하고 싶을 때 되살려 사용).
"""

from __future__ import annotations

import json
from io import BytesIO
from pathlib import Path
from typing import Protocol

import numpy as np
from fastapi import UploadFile
from PIL import Image, UnidentifiedImageError

from app.core.config import Settings
from app.core.errors import InvalidImageError
from app.schemas.analysis import AnalysisResponse, Detection
from app.services.onnx_engine import OnnxYoloEngine

_SEVERITY_RANK = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}


class DiseasePredictor(Protocol):
    def predict_sync(self, file_bytes: bytes, filename: str | None) -> AnalysisResponse: ...


def _load_labels(settings: Settings) -> list[dict]:
    if not settings.labels_path or not Path(settings.labels_path).is_file():
        return []
    return json.loads(Path(settings.labels_path).read_text(encoding="utf-8")).get("classes", [])


def _decode_image_array(file_bytes: bytes) -> np.ndarray:
    """원본 RGB(HWC) 배열 반환. 디코딩 불가 시 InvalidImageError."""
    if not file_bytes:
        raise InvalidImageError("빈 파일입니다.")
    try:
        with Image.open(BytesIO(file_bytes)) as image:
            return np.asarray(image.convert("RGB"))
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise InvalidImageError("이미지를 디코딩할 수 없습니다.") from exc


class OnnxDiseasePredictor:
    """ONNX(YOLO26) 추론기. 무거운 추론은 라우터에서 스레드풀 + 동시성 제한으로 위임한다.

    모델은 '포도 전용'(노균병/탄저병). 탐지 0건이면 '정상'으로 본다.
    출력 좌표(bbox)는 클라이언트가 원본에 박스를 그리도록 그대로 전달한다.
    """

    def __init__(self, settings: Settings, engine: OnnxYoloEngine) -> None:
        self._settings = settings
        self._engine = engine
        self._labels = _load_labels(settings)

    def _label(self, class_id: int) -> tuple[str, str, str]:
        if 0 <= class_id < len(self._labels):
            c = self._labels[class_id]
            return (
                c.get("name", f"class_{class_id}"),
                c.get("name_ko", f"클래스 {class_id}"),
                c.get("severity", "LOW"),
            )
        return f"class_{class_id}", f"클래스 {class_id}", "LOW"

    def predict_sync(self, file_bytes: bytes, filename: str | None = None) -> AnalysisResponse:
        image = _decode_image_array(file_bytes)
        h, w = image.shape[:2]

        dets: list[Detection] = []          # 클라이언트 렌더링용(bbox 포함)
        meta: list[tuple[str, float, str]] = []  # 집계용 (label_ko, conf, severity)
        for class_id, conf, bbox in self._engine.infer(image):  # bbox=[x,y,w,h] 원본 좌표
            name, label_ko, severity = self._label(class_id)
            dets.append(Detection(class_name=name, label=label_ko, confidence=conf, bbox=bbox))
            meta.append((label_ko, conf, severity))

        order = sorted(range(len(meta)), key=lambda i: meta[i][1], reverse=True)
        dets = [dets[i] for i in order]
        meta = [meta[i] for i in order]
        return self._aggregate(dets, meta, w, h)

    def _aggregate(
        self, dets: list[Detection], meta: list[tuple[str, float, str]], w: int, h: int
    ) -> AnalysisResponse:
        image_size = {"width": w, "height": h}
        if not meta:  # 탐지 0건 = 정상 (별도 클래스 아님)
            return AnalysisResponse(
                diagnosis="정상", confidence=0.0, severity="LOW",
                summary="포도 잎에서 병충해가 탐지되지 않았습니다(정상).",
                recommended_action="", model_version=self._settings.model_version,
                detections=[], detection_count=0, image_size=image_size,
            )
        top = meta[0]
        worst = max(meta, key=lambda m: _SEVERITY_RANK.get(m[2], 0))
        return AnalysisResponse(
            diagnosis=top[0], confidence=top[1], severity=worst[2],
            summary=f"{len(meta)}건의 병충해가 탐지되었습니다(대표: {top[0]}).",
            recommended_action="",  # 대응문구는 Spring RAG 가 생성(팀 합의)
            model_version=self._settings.model_version,
            detections=dets, detection_count=len(dets), image_size=image_size,
        )

    def info(self) -> dict:
        return {
            "mode": "onnx",
            "model_loaded": True,
            "model_version": self._settings.model_version,
            "classes": [c.get("name_ko", c.get("name")) for c in self._labels],
            "providers": getattr(self._engine, "active_providers", None),
        }

    async def predict(self, file: UploadFile) -> AnalysisResponse:
        return self.predict_sync(await file.read(), file.filename)


def build_predictor(settings: Settings) -> OnnxDiseasePredictor:
    """ONNX 모델을 로드해 추론기를 만든다. 모델이 없으면 명확히 실패(마운트/경로 확인용)."""
    model_path = settings.resolved_model_path()
    if model_path is None:
        raise RuntimeError(
            f"ONNX 모델을 찾을 수 없습니다 (AI_MODEL_PATH={settings.model_path}). "
            "models 볼륨 마운트와 경로를 확인하세요."
        )
    engine = OnnxYoloEngine(str(model_path), settings)
    return OnnxDiseasePredictor(settings, engine)


# =============================================================================
# 목업(사전정의 결과) — 모델/추론 의존성 없이 통신만 검증할 때 되살려 사용.
# 활성화: 아래 클래스 주석 해제 + build_predictor 를 목업 반환으로 교체.
#
# def _image_size(file_bytes: bytes) -> tuple[int, int]:
#     if not file_bytes:
#         raise InvalidImageError("빈 파일입니다.")
#     try:
#         with Image.open(BytesIO(file_bytes)) as image:
#             return image.size
#     except (UnidentifiedImageError, OSError, ValueError) as exc:
#         raise InvalidImageError("이미지를 디코딩할 수 없습니다.") from exc
#
#
# class GrapeMockDiseasePredictor:
#     """실행용 목업 — dev 더미와 동일한 사전정의 결과 + 데모 bbox."""
#
#     def __init__(self, settings: Settings) -> None:
#         self._settings = settings
#
#     def predict_sync(self, file_bytes: bytes, filename: str | None = None) -> AnalysisResponse:
#         width, height = _image_size(file_bytes)
#         bbox = [round(width * 0.3, 1), round(height * 0.3, 1), round(width * 0.4, 1), round(height * 0.4, 1)]
#         detections = [Detection(class_name="grape_disease", label="포도 병충해(더미)", confidence=0.87, bbox=bbox)]
#         return AnalysisResponse(
#             diagnosis="Grape disease suspicion (dummy)", confidence=0.87, severity="LOW",
#             summary=("This MVP dummy predictor represents the grape disease analysis flow. "
#                      "A real grape ML model can replace GrapeMockDiseasePredictor later."),
#             recommended_action=("Inspect grape leaves and clusters, isolate suspicious vines if needed, "
#                                 "and review grape disease guidance before applying treatment."),
#             model_version=f"grape-dummy-disease-predictor-v1:{len(file_bytes)}:{width}x{height}",
#             detections=detections, detection_count=len(detections),
#             image_size={"width": width, "height": height},
#         )
#
#     def info(self) -> dict:
#         return {"mode": "mock", "model_loaded": False, "model_version": "grape-dummy-disease-predictor-v1"}
#
#     async def predict(self, file: UploadFile) -> AnalysisResponse:
#         return self.predict_sync(await file.read(), file.filename)
#
#
# def build_predictor(settings: Settings):   # 목업 버전(위 ONNX build_predictor 와 택일)
#     return GrapeMockDiseasePredictor(settings)
# =============================================================================
