"""추론기 정의. ONNX 모델이 있으면 OnnxDiseasePredictor, 없으면 DummyDiseasePredictor."""

from __future__ import annotations

import json
import time
from io import BytesIO
from pathlib import Path
from typing import Protocol

import numpy as np
from fastapi import UploadFile
from PIL import Image, UnidentifiedImageError

from app.core.config import Settings
from app.core.errors import InvalidImageError
from app.schemas.analysis import AnalysisResponse, Detection

_SEVERITY_RANK = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
_DEMO_CLASSES = ["노균병", "탄저병"]


class DiseasePredictor(Protocol):
    def predict_sync(self, file_bytes: bytes, filename: str | None) -> AnalysisResponse: ...


def _load_labels(settings: Settings) -> list[dict]:
    if not settings.labels_path or not Path(settings.labels_path).is_file():
        return []
    data = json.loads(Path(settings.labels_path).read_text(encoding="utf-8"))
    return data.get("classes", [])


def _decode_image(file_bytes: bytes) -> np.ndarray:
    if not file_bytes:
        raise InvalidImageError("빈 파일입니다.")
    try:
        with Image.open(BytesIO(file_bytes)) as image:
            return np.asarray(image.convert("RGB"))
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise InvalidImageError("이미지를 디코딩할 수 없습니다.") from exc


class OnnxDiseasePredictor:
    """ONNX 엔진으로 추론. 무거운 추론은 호출 측에서 스레드풀로 위임한다(아래 라우터 참고)."""

    def __init__(self, settings: Settings, engine) -> None:
        self._settings = settings
        self._engine = engine
        self._labels = _load_labels(settings)

    def _label(self, class_id: int) -> tuple[str, str, str]:
        if 0 <= class_id < len(self._labels):
            c = self._labels[class_id]
            return c.get("name", f"class_{class_id}"), c.get("name_ko", f"클래스 {class_id}"), c.get("severity", "LOW")
        return f"class_{class_id}", f"클래스 {class_id}", "LOW"

    def predict_sync(self, file_bytes: bytes, filename: str | None = None) -> AnalysisResponse:
        started = time.perf_counter()
        image = _decode_image(file_bytes)
        h, w = image.shape[:2]

        raw = self._engine.infer(image)
        detections: list[Detection] = []
        for class_id, conf, bbox in raw:
            name, name_ko, severity = self._label(class_id)
            detections.append(
                Detection(class_name=name, label_ko=name_ko, confidence=conf, severity=severity, bbox=bbox)
            )
        # 신뢰도 내림차순
        detections.sort(key=lambda d: d.confidence, reverse=True)

        elapsed_ms = (time.perf_counter() - started) * 1000
        return self._aggregate(detections, w, h, elapsed_ms)

    def _aggregate(self, detections: list[Detection], w: int, h: int, elapsed_ms: float) -> AnalysisResponse:
        if not detections:
            return AnalysisResponse(
                diagnosis="정상", confidence=0.0, severity="LOW",
                summary="포도 잎에서 병충해가 탐지되지 않았습니다(정상).", recommended_action="",
                model_version=self._settings.model_version,
                detections=[], detection_count=0,
                image_size={"width": w, "height": h}, inference_time_ms=round(elapsed_ms, 2),
            )
        top = detections[0]
        worst = max(detections, key=lambda d: _SEVERITY_RANK.get(d.severity, 0))
        return AnalysisResponse(
            diagnosis=top.label_ko,
            confidence=top.confidence,
            severity=worst.severity,
            summary=f"{len(detections)}건의 병충해가 탐지되었습니다(대표: {top.label_ko}).",
            recommended_action="",  # 대응 문구는 백엔드 RAG 모듈이 생성(팀 합의 사항)
            model_version=self._settings.model_version,
            detections=detections,
            detection_count=len(detections),
            image_size={"width": w, "height": h},
            inference_time_ms=round(elapsed_ms, 2),
        )

    def info(self) -> dict:
        return {
            "mode": "onnx",
            "model_loaded": True,
            "model_version": self._settings.model_version,
            "classes": [c.get("name_ko", c.get("name")) for c in self._labels],
            "input_size": self._settings.input_size,
            "conf_threshold": self._settings.conf_threshold,
            "iou_threshold": self._settings.iou_threshold,
            "nms_free": self._settings.nms_free,
        }

    async def predict(self, file: UploadFile) -> AnalysisResponse:  # 직접 호출 시 동기 경로 사용
        return self.predict_sync(await file.read(), file.filename)


# 포도 전용 데모 클래스 (파일명 키워드 → 진단). 실제 .onnx 탑재 전 시연/발표용.
# 모델은 "포도 정상 vs 병충해"만 판별한다는 전제(다작물 비대상).
_GRAPE_DISEASES: list[tuple[tuple[str, ...], str, str, str, float]] = [
    # (파일명 키워드들, class_name, 한글명, severity, 데모 신뢰도)
    # 학습 클래스는 2개(노균병/탄저병). '정상'은 클래스가 아니라 탐지 0건으로 처리.
    (("downy", "노균"), "downy_mildew", "노균병", "HIGH", 0.92),
    (("anthracnose", "탄저"), "anthracnose", "탄저병", "MEDIUM", 0.78),
]
_HEALTHY_KEYWORDS = ("healthy", "normal", "정상")


class DummyDiseasePredictor:
    """포도 전용 데모 더미. 파일명 키워드로 정상/병명을 분기한다.

    실제 ONNX 모델 탑재 전, 발표/시연에서 '포도 선택 → 추론 결과' 흐름을 보여주기 위한 스탠드인.
    출력은 실제 모델과 동일한 API 계약(AnalysisResponse + detections)을 따른다.
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def predict_sync(self, file_bytes: bytes, filename: str | None = None) -> AnalysisResponse:
        started = time.perf_counter()
        image = _decode_image(file_bytes)  # 잘못된 이미지면 InvalidImageError
        h, w = image.shape[:2]

        name = (filename or "").lower()
        elapsed_ms = (time.perf_counter() - started) * 1000
        version = f"{self._settings.model_version}-demo"

        # 1) 정상 키워드 → 병충해 없음
        if any(k in name for k in _HEALTHY_KEYWORDS):
            return AnalysisResponse(
                diagnosis="정상", confidence=0.96, severity="LOW",
                summary="포도 잎에서 병충해가 탐지되지 않았습니다(정상).",
                recommended_action="", model_version=version,
                detections=[], detection_count=0,
                image_size={"width": w, "height": h}, inference_time_ms=round(elapsed_ms, 2),
            )

        # 2) 병명 키워드 매칭 (없으면 기본=노균병)
        keywords, class_name, label_ko, severity, conf = next(
            (d for d in _GRAPE_DISEASES if any(k in name for k in d[0])),
            _GRAPE_DISEASES[0],
        )
        # 데모용 박스: 이미지 중앙 약 40% 영역
        bbox = [round(w * 0.3, 1), round(h * 0.3, 1), round(w * 0.4, 1), round(h * 0.4, 1)]
        detection = Detection(
            class_name=class_name, label_ko=label_ko, confidence=conf, severity=severity, bbox=bbox
        )
        return AnalysisResponse(
            diagnosis=label_ko, confidence=conf, severity=severity,
            summary=f"포도 {label_ko} 의심 병반이 탐지되었습니다(데모).",
            recommended_action="", model_version=version,
            detections=[detection], detection_count=1,
            image_size={"width": w, "height": h}, inference_time_ms=round(elapsed_ms, 2),
        )

    def info(self) -> dict:
        return {
            "mode": "demo",
            "model_loaded": False,
            "model_version": f"{self._settings.model_version}-demo",
            "classes": _DEMO_CLASSES,
            "input_size": self._settings.input_size,
            "conf_threshold": self._settings.conf_threshold,
            "iou_threshold": self._settings.iou_threshold,
            "nms_free": self._settings.nms_free,
        }

    async def predict(self, file: UploadFile) -> AnalysisResponse:
        return self.predict_sync(await file.read(), file.filename)


def build_predictor(settings: Settings):
    """설정에 따라 적절한 추론기를 생성. 모델 로드 실패 시 더미로 안전 폴백."""
    model_path = settings.resolved_model_path()
    if model_path is None:
        return DummyDiseasePredictor(settings)
    try:
        from app.services.onnx_engine import OnnxYoloEngine

        engine = OnnxYoloEngine(str(model_path), settings)
        return OnnxDiseasePredictor(settings, engine)
    except Exception as exc:  # 모델 손상/런타임 문제 → 기동은 유지, 더미로 폴백
        import logging

        logging.getLogger("uvicorn.error").warning("ONNX 모델 로드 실패, 더미로 폴백: %s", exc)
        return DummyDiseasePredictor(settings)
