import os
from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="AI_", extra="ignore")

    # ── 모델 ────────────────────────────────────────────────
    # .onnx 모델 경로(기본값=이미지에 구운 표준 경로). 비밀이 아니므로 .env 없이 기본값으로 동작.
    # 다른 모델로 교체 시에만 AI_MODEL_PATH 로 오버라이드(또는 볼륨 마운트).
    model_path: str | None = "models/yolo26-grape.onnx"
    model_version: str = "yolo26-grape-onnx-v1"

    # 클래스 라벨 정의 파일(JSON). 형식은 labels.example.json 참고.
    labels_path: str | None = "models/labels.json"

    # ── 추론 파라미터 ──────────────────────────────────────
    input_size: int = 640          # 정사각 입력 (letterbox)
    conf_threshold: float = 0.25   # 신뢰도 컷오프
    iou_threshold: float = 0.45    # NMS IoU (raw 출력일 때만 사용)

    # 모델 출력 형태.
    #   None = 자동 감지(출력 shape 으로 판별)
    #   True  = end-to-end(NMS 내장) → 후처리에서 NMS 생략
    #   False = raw 출력 → 코드에서 NMS 수행
    nms_free: bool | None = None

    # 추론 디바이스: "auto"(GPU 가능하면 GPU, 아니면 CPU 폴백) | "cpu" | "cuda".
    # GPU 사용은 onnxruntime-gpu 패키지 + CUDA 런타임이 있어야 실제로 활성화됨.
    device: str = "auto"

    # ── ONNX Runtime 최적화 (CPU) ──────────────────────────
    # 0 = ORT 자동. 동시요청이 많으면 세션당 스레드를 2~4로 제한해 처리량 확보.
    intra_op_num_threads: int = Field(default=0, ge=0)
    inter_op_num_threads: int = Field(default=1, ge=0)

    # ── 운영(동시성/입력 제한) ─────────────────────────────
    # 동시에 수행할 최대 추론 수(CPU 보호). 0 = CPU 코어수 기반 자동.
    max_concurrency: int = Field(default=0, ge=0)
    # 업로드 허용 최대 크기(MB). 초과 시 413.
    max_upload_mb: float = Field(default=10.0, gt=0)
    # CORS 허용 오리진(쉼표 구분). 비우면 비활성(서버-서버 호출이면 불필요).
    cors_allow_origins: str = ""

    # ── 디버그(검증용) ─────────────────────────────────────
    # True 면 추론 후 '입력 이미지 + bbox 그린 결과'를 annotated_dir 에 저장한다.
    # API 응답에는 영향 없음(좌표만 반환). 검증/시연 용도.
    save_annotated: bool = False
    annotated_dir: str = "debug"

    def resolved_model_path(self) -> Path | None:
        if not self.model_path:
            return None
        path = Path(self.model_path)
        return path if path.is_file() else None

    def resolved_max_concurrency(self) -> int:
        if self.max_concurrency > 0:
            return self.max_concurrency
        return max(1, (os.cpu_count() or 2))

    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allow_origins.split(",") if o.strip()]

    @property
    def max_upload_bytes(self) -> int:
        return int(self.max_upload_mb * 1024 * 1024)


@lru_cache
def get_settings() -> Settings:
    return Settings()
