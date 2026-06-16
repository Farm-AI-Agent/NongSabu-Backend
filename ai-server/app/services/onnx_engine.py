"""ONNX Runtime 기반 YOLO26 추론 엔진 (CPU).

런타임 의존성은 numpy + onnxruntime 뿐이다(torch/ultralytics 불필요).
모델은 GPU 서버에서 미리 .onnx 로 변환·최적화된 것을 로드해 추론만 수행한다.
"""

from __future__ import annotations

import numpy as np
import onnxruntime as ort

from app.core.config import Settings


def _letterbox(image: np.ndarray, new_size: int) -> tuple[np.ndarray, float, tuple[int, int]]:
    """비율을 유지하며 정사각(new_size)으로 패딩. (전처리 이미지, scale, (pad_x, pad_y)) 반환."""
    h, w = image.shape[:2]
    scale = min(new_size / h, new_size / w)
    nh, nw = round(h * scale), round(w * scale)

    resized = np.asarray(
        _resize(image, nw, nh), dtype=np.float32
    )
    canvas = np.full((new_size, new_size, 3), 114.0, dtype=np.float32)
    pad_x, pad_y = (new_size - nw) // 2, (new_size - nh) // 2
    canvas[pad_y:pad_y + nh, pad_x:pad_x + nw] = resized
    return canvas, scale, (pad_x, pad_y)


def _resize(image: np.ndarray, nw: int, nh: int) -> np.ndarray:
    """Pillow 로 리사이즈(추가 의존성 없이). image 는 HWC RGB uint8."""
    from PIL import Image

    return np.asarray(Image.fromarray(image).resize((nw, nh), Image.BILINEAR))


def _nms(boxes: np.ndarray, scores: np.ndarray, iou_threshold: float) -> list[int]:
    """단순 numpy NMS. boxes 는 [N,4] = (x1,y1,x2,y2). 유지할 인덱스 목록 반환."""
    if boxes.size == 0:
        return []
    x1, y1, x2, y2 = boxes[:, 0], boxes[:, 1], boxes[:, 2], boxes[:, 3]
    areas = (x2 - x1).clip(min=0) * (y2 - y1).clip(min=0)
    order = scores.argsort()[::-1]

    keep: list[int] = []
    while order.size > 0:
        i = int(order[0])
        keep.append(i)
        if order.size == 1:
            break
        xx1 = np.maximum(x1[i], x1[order[1:]])
        yy1 = np.maximum(y1[i], y1[order[1:]])
        xx2 = np.minimum(x2[i], x2[order[1:]])
        yy2 = np.minimum(y2[i], y2[order[1:]])
        inter = (xx2 - xx1).clip(min=0) * (yy2 - yy1).clip(min=0)
        iou = inter / (areas[i] + areas[order[1:]] - inter + 1e-9)
        order = order[1:][iou <= iou_threshold]
    return keep


def _select_providers(settings: Settings) -> list[str]:
    """환경에 맞는 실행 공급자(EP) 목록을 자동 선택. GPU 가능하면 GPU, 아니면 CPU 폴백.

    - device="auto"(기본): 사용 가능한 EP 중 GPU(CUDA/TensorRT)가 있으면 우선, 없으면 CPU.
    - device="cpu": 무조건 CPU.
    - device="cuda": CUDA 우선(없으면 CPU 폴백).

    주의: GPU EP 는 `onnxruntime-gpu` 패키지 + CUDA/cuDNN 런타임이 있어야 노출된다.
    CPU 전용 `onnxruntime` 에서는 CUDA EP 가 목록에 없으므로 자동으로 CPU 만 사용한다.
    """
    available = ort.get_available_providers()
    cpu = "CPUExecutionProvider"
    gpu_priority = ["TensorrtExecutionProvider", "CUDAExecutionProvider"]

    device = getattr(settings, "device", "auto")
    if device == "cpu":
        return [cpu]

    wanted = [p for p in gpu_priority if p in available]
    if device == "cuda" and "CUDAExecutionProvider" in available:
        wanted = ["CUDAExecutionProvider"]
    return [*wanted, cpu] if wanted else [cpu]


class OnnxYoloEngine:
    """ONNX YOLO 추론 세션 래퍼. 스레드 세이프한 run() 을 제공."""

    def __init__(self, model_path: str, settings: Settings) -> None:
        self._settings = settings

        opts = ort.SessionOptions()
        opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        opts.intra_op_num_threads = settings.intra_op_num_threads
        opts.inter_op_num_threads = settings.inter_op_num_threads

        self.session = ort.InferenceSession(
            model_path, sess_options=opts, providers=_select_providers(settings)
        )
        self._input_name = self.session.get_inputs()[0].name
        self._size = settings.input_size
        # 실제로 어떤 EP 가 적용됐는지 (GPU 폴백 여부 확인용)
        self.active_providers = self.session.get_providers()

    def infer(self, image_rgb: np.ndarray) -> list[tuple[int, float, list[float]]]:
        """원본 RGB(HWC uint8) 이미지를 받아 (class_id, confidence, [x,y,w,h]) 목록 반환.

        end-to-end(NMS 내장) 모델과 raw 모델을 모두 지원한다.
        """
        h0, w0 = image_rgb.shape[:2]
        canvas, scale, (pad_x, pad_y) = _letterbox(image_rgb, self._size)

        # HWC -> CHW, /255, 배치 차원 추가
        tensor = (canvas / 255.0).transpose(2, 0, 1)[None].astype(np.float32)
        outputs = self.session.run(None, {self._input_name: tensor})
        arr = self._orient(outputs[0])  # [rows, cols]

        if self._is_end_to_end(arr):
            return self._decode_e2e(arr, scale, pad_x, pad_y, w0, h0)
        return self._decode_raw(arr, scale, pad_x, pad_y, w0, h0)

    @staticmethod
    def _orient(out: np.ndarray) -> np.ndarray:
        """(1, A, B) -> (rows, cols). rows=탐지/앵커 축, cols=피처 축이 되도록 정렬."""
        arr = np.squeeze(out, axis=0) if out.ndim == 3 else out
        if arr.ndim == 1:
            arr = arr[None, :]
        # 피처 축(작은 쪽)이 cols 가 되도록: rows >= cols
        if arr.shape[0] < arr.shape[1]:
            arr = arr.transpose()
        return arr

    def _is_end_to_end(self, arr: np.ndarray) -> bool:
        """NMS-free(end-to-end) 출력 여부.

        설정으로 강제 가능. 자동 감지: end-to-end 는 (top-k, 6) = (x1,y1,x2,y2,conf,cls)
        형태로 행 수가 적고(<=1000) cols==6. raw 는 앵커 수가 수천(>1000)이라 행이 많다.
        """
        if self._settings.nms_free is not None:
            return self._settings.nms_free
        rows, cols = arr.shape
        return cols == 6 and rows <= 1000

    def _decode_e2e(
        self, arr: np.ndarray, scale: float, pad_x: int, pad_y: int, w0: int, h0: int
    ) -> list[tuple[int, float, list[float]]]:
        """NMS 내장 출력: [N, 6] = (x1, y1, x2, y2, conf, cls). NMS 불필요."""
        results: list[tuple[int, float, list[float]]] = []
        for row in arr:
            conf = float(row[4])
            if conf < self._settings.conf_threshold:
                continue
            x1, y1, x2, y2 = self._scale_box(row[:4], scale, pad_x, pad_y, w0, h0)
            results.append((int(round(row[5])), conf, [x1, y1, x2 - x1, y2 - y1]))
        return results

    def _decode_raw(
        self, preds: np.ndarray, scale: float, pad_x: int, pad_y: int, w0: int, h0: int
    ) -> list[tuple[int, float, list[float]]]:
        """raw 출력: [N, 4+nc] = (cx,cy,w,h, class_scores...). 코드에서 NMS 수행."""
        boxes, scores, class_ids = self._decode(preds, scale, pad_x, pad_y, w0, h0)
        if not len(boxes):
            return []
        keep = _nms(boxes, scores, self._settings.iou_threshold)
        results: list[tuple[int, float, list[float]]] = []
        for i in keep:
            x1, y1, x2, y2 = boxes[i]
            results.append(
                (int(class_ids[i]), float(scores[i]), [float(x1), float(y1), float(x2 - x1), float(y2 - y1)])
            )
        return results

    @staticmethod
    def _scale_box(xyxy, scale: float, pad_x: int, pad_y: int, w0: int, h0: int) -> tuple[float, float, float, float]:
        x1 = float(np.clip((xyxy[0] - pad_x) / scale, 0, w0))
        y1 = float(np.clip((xyxy[1] - pad_y) / scale, 0, h0))
        x2 = float(np.clip((xyxy[2] - pad_x) / scale, 0, w0))
        y2 = float(np.clip((xyxy[3] - pad_y) / scale, 0, h0))
        return x1, y1, x2, y2

    def _decode(
        self, preds: np.ndarray, scale: float, pad_x: int, pad_y: int, w0: int, h0: int
    ) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        if preds.shape[0] == 0 or preds.shape[1] < 5:
            empty = np.empty((0, 4))
            return empty, np.empty((0,)), np.empty((0,))

        boxes_xywh = preds[:, :4]
        class_scores = preds[:, 4:]
        class_ids = class_scores.argmax(axis=1)
        confidences = class_scores.max(axis=1)

        mask = confidences >= self._settings.conf_threshold
        boxes_xywh, confidences, class_ids = boxes_xywh[mask], confidences[mask], class_ids[mask]
        if not len(boxes_xywh):
            return np.empty((0, 4)), np.empty((0,)), np.empty((0,))

        # letterbox 좌표 -> 원본 좌표. (cx,cy,w,h) -> (x1,y1,x2,y2)
        cx, cy, w, h = boxes_xywh.T
        x1 = (cx - w / 2 - pad_x) / scale
        y1 = (cy - h / 2 - pad_y) / scale
        x2 = (cx + w / 2 - pad_x) / scale
        y2 = (cy + h / 2 - pad_y) / scale
        boxes = np.stack([x1, y1, x2, y2], axis=1)
        boxes[:, [0, 2]] = boxes[:, [0, 2]].clip(0, w0)
        boxes[:, [1, 3]] = boxes[:, [1, 3]].clip(0, h0)
        return boxes, confidences, class_ids
