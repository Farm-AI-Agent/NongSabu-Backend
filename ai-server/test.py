"""ai-server 동작 검증 테스트 (HTTP 업로드, curl 과 동일한 multipart 요청).

이미지(또는 폴더)를 ai-server 에 업로드하고, 다음 결과물을 test_results/ 에 저장한다:
  1) <이름>_annotated.jpg  — 입력 이미지 + bbox 가 그려진 이미지
  2) <이름>_result.json    — 전체 응답(분류 결과 + bbox 정보)
  3) 콘솔 요약              — 진단/심각도/탐지수/박스 좌표

사용:
  uv run python test.py <이미지_또는_폴더> [--url http://localhost:8000] [--out test_results]

의존성: 표준 라이브러리(urllib) + Pillow 만 사용(추가 설치 불필요).
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

PREDICT_PATH = "/api/v1/disease/predict"
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
_FONT_CANDIDATES = [
    r"C:\Windows\Fonts\malgun.ttf",
    "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
    "/System/Library/Fonts/AppleSDGothicNeo.ttc",
]


def _load_font(size: int):
    for path in _FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size), True
        except OSError:
            continue
    return ImageFont.load_default(), False


def _post_image(url: str, path: Path) -> tuple[int, dict]:
    """multipart/form-data 로 이미지 업로드(curl -F file=@... 와 동일). (status, json) 반환."""
    boundary = uuid.uuid4().hex
    ctype = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
        f"Content-Type: {ctype}\r\n\r\n"
    ).encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()

    req = urllib.request.Request(
        f"{url}{PREDICT_PATH}", data=body, method="POST",
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read() or b"{}")


def _draw_annotated(src: Path, detections: list, dest: Path) -> None:
    image = Image.open(src).convert("RGB")
    draw = ImageDraw.Draw(image)
    font, ko = _load_font(max(16, image.width // 60))
    lw = max(2, image.width // 400)
    fsz = getattr(font, "size", 14)
    for d in detections:
        x, y, w, h = d["bbox"]
        draw.rectangle([x, y, x + w, y + h], outline=(255, 0, 0), width=lw)
        text = f"{(d['label'] if ko else d['class_name'])} {d['confidence']:.2f}"
        ty = max(0, y - fsz - 4)
        draw.rectangle([x, ty, x + draw.textlength(text, font=font) + 6, ty + fsz + 4], fill=(255, 0, 0))
        draw.text((x + 3, ty + 1), text, fill=(255, 255, 255), font=font)
    image.save(dest, quality=90)


def _run_one(url: str, src: Path, out_dir: Path) -> bool:
    status, body = _post_image(url, src)
    if status != 200:
        print(f"[FAIL] {src.name}: HTTP {status} {body}")
        return False

    out_dir.mkdir(parents=True, exist_ok=True)
    # 2) 전체 결과(JSON): 분류 결과 + bbox 정보
    (out_dir / f"{src.stem}_result.json").write_text(
        json.dumps(body, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    # 1) annotated 이미지
    _draw_annotated(src, body.get("detections", []), out_dir / f"{src.stem}_annotated.jpg")

    # 3) 콘솔 요약
    print(f"[OK]   {src.name}")
    print(f"       진단={body['diagnosis']}  심각도={body['severity']}  "
          f"신뢰도={body['confidence']:.3f}  탐지={body['detection_count']}건")
    for i, d in enumerate(body.get("detections", []), 1):
        bx = [round(v, 1) for v in d["bbox"]]
        print(f"         #{i} {d['label']}({d['class_name']}) conf={d['confidence']:.3f} bbox(x,y,w,h)={bx}")
    return True


def main() -> int:
    ap = argparse.ArgumentParser(description="ai-server 검증 테스트")
    ap.add_argument("target", help="이미지 파일 또는 폴더")
    ap.add_argument("--url", default="http://localhost:8000", help="ai-server 베이스 URL")
    ap.add_argument("--out", default="test_results", help="결과 저장 폴더")
    args = ap.parse_args()

    target = Path(args.target)
    if target.is_dir():
        images = sorted(p for p in target.iterdir() if p.suffix.lower() in IMAGE_EXTS)
    elif target.is_file():
        images = [target]
    else:
        print(f"[에러] 경로 없음: {target}")
        return 1
    if not images:
        print(f"[에러] 이미지 없음: {target}")
        return 1

    out_dir = Path(args.out)
    print(f"ai-server: {args.url}  |  대상 {len(images)}개  |  결과: {out_dir}/\n")
    ok = sum(_run_one(args.url, img, out_dir) for img in images)
    print(f"\n완료: {ok}/{len(images)} 성공 → {out_dir}/ (annotated.jpg + result.json)")
    return 0 if ok == len(images) else 1


if __name__ == "__main__":
    sys.exit(main())
