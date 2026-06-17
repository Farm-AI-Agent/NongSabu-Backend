"""ai-server 검증용 시각화 도구.

실행 중인 ai-server 에 이미지를 POST → 응답(bbox/클래스)을 받아 입력 이미지에
박스를 그려 저장한다. ai-server 가 제대로 추론하는지 눈으로 확인하는 용도.
(API 계약은 좌표만 반환하며, 이 그리기는 클라이언트/디버그 측 책임이다.)

사용:
  uv run python scripts/visualize_detection.py <이미지경로> [--url http://localhost:8000] [--out 출력경로]
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import httpx
from PIL import Image, ImageDraw, ImageFont

PREDICT = "/api/v1/disease/predict"
_FONT_CANDIDATES = [
    r"C:\Windows\Fonts\malgun.ttf",          # Windows 한글
    "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
    "/System/Library/Fonts/AppleSDGothicNeo.ttc",
]


def _load_font(size: int):
    for path in _FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size), True  # 한글 가능
        except OSError:
            continue
    return ImageFont.load_default(), False  # 폴백(한글 불가 → 영문 라벨 사용)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("image", help="입력 이미지 경로")
    ap.add_argument("--url", default="http://localhost:8000", help="ai-server 베이스 URL")
    ap.add_argument("--out", default=None, help="출력 이미지 경로(기본: <이름>_annotated.jpg)")
    ap.add_argument("--conf", type=float, default=0.0, help="표시 최소 신뢰도")
    args = ap.parse_args()

    src = Path(args.image)
    if not src.is_file():
        print(f"[에러] 이미지 없음: {src}")
        return 1
    out = Path(args.out) if args.out else src.with_name(f"{src.stem}_annotated.jpg")

    # 1) ai-server 추론 요청
    with open(src, "rb") as f:
        resp = httpx.post(
            f"{args.url}{PREDICT}",
            files={"file": (src.name, f.read(), "image/jpeg")},
            timeout=60.0,
        )
    if resp.status_code != 200:
        print(f"[에러] {resp.status_code}: {resp.text}")
        return 1
    body = resp.json()

    print(f"diagnosis      : {body.get('diagnosis')}")
    print(f"severity       : {body.get('severity')}  confidence: {body.get('confidence')}")
    print(f"detection_count: {body.get('detection_count')}  image_size: {body.get('image_size')}")

    # 2) 원본에 박스 그리기
    image = Image.open(src).convert("RGB")
    draw = ImageDraw.Draw(image)
    font, ko_ok = _load_font(max(16, image.width // 60))
    line_w = max(2, image.width // 400)

    drawn = 0
    for d in body.get("detections", []):
        if d.get("confidence", 0) < args.conf:
            continue
        x, y, w, h = d["bbox"]
        draw.rectangle([x, y, x + w, y + h], outline=(255, 0, 0), width=line_w)
        text = f"{(d['label'] if ko_ok else d['class_name'])} {d['confidence']:.2f}"
        ty = max(0, y - (font.size + 4 if hasattr(font, "size") else 14))
        draw.rectangle([x, ty, x + draw.textlength(text, font=font) + 6, ty + (font.size if hasattr(font, "size") else 12) + 4], fill=(255, 0, 0))
        draw.text((x + 3, ty + 1), text, fill=(255, 255, 255), font=font)
        drawn += 1

    if drawn == 0:
        print("(탐지 0건 — 박스 없음. 정상 이미지면 정상 동작)")
    image.save(out, quality=90)
    print(f"저장: {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
