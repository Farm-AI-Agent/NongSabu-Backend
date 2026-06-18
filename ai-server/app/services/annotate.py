"""검증용 시각화 — 입력 이미지에 bbox/라벨을 그려 디버그 폴더에 저장.

API 응답(좌표만 반환)과 무관한 부가 디버그 출력이다. AI_SAVE_ANNOTATED=true 일 때만 동작.
"""

from __future__ import annotations

import logging
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

logger = logging.getLogger("ai-server")

# 한글 폰트 후보 (컨테이너: fonts-nanum / Windows 로컬: 맑은 고딕)
_FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
    "/usr/share/fonts/opentype/nanum/NanumGothic.ttf",
    r"C:\Windows\Fonts\malgun.ttf",
]


def _load_font(size: int):
    for path in _FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size), True   # 한글 가능
        except OSError:
            continue
    return ImageFont.load_default(), False                # 폴백 → 영문 라벨


def save_annotated(image_bytes: bytes, detections: list, out_dir: str, filename: str | None) -> str | None:
    """원본에 박스/라벨을 그려 저장하고 경로를 반환. 실패해도 추론에 영향 주지 않음."""
    try:
        image = Image.open(BytesIO(image_bytes)).convert("RGB")
    except Exception as exc:
        logger.warning("annotated 저장 스킵(이미지 디코딩 실패): %s", exc)
        return None

    draw = ImageDraw.Draw(image)
    font, ko_ok = _load_font(max(16, image.width // 60))
    line_w = max(2, image.width // 400)
    fsz = getattr(font, "size", 14)

    for d in detections:
        x, y, w, h = d.bbox
        draw.rectangle([x, y, x + w, y + h], outline=(255, 0, 0), width=line_w)
        text = f"{(d.label if ko_ok else d.class_name)} {d.confidence:.2f}"
        ty = max(0, y - fsz - 4)
        tw = draw.textlength(text, font=font)
        draw.rectangle([x, ty, x + tw + 6, ty + fsz + 4], fill=(255, 0, 0))
        draw.text((x + 3, ty + 1), text, fill=(255, 255, 255), font=font)

    out_path = Path(out_dir)
    out_path.mkdir(parents=True, exist_ok=True)
    stem = Path(filename or "image").stem or "image"
    dest = out_path / f"{stem}_annotated.jpg"
    image.save(dest, quality=90)
    logger.info("annotated 저장: %s (탐지 %d건)", dest, len(detections))
    return str(dest)
