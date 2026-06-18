# ai-server/models — 모델 가중치 배치 위치

이 폴더에 추론 모델(ONNX)을 둔다. **모델 가중치(`*.onnx`, `*.pt`)는 용량 때문에 git 에 올리지 않는다.**
대신 `labels.json` 과 이 README 는 git 에 포함되어 있어, **팀원은 onnx 파일만 받아 여기에 넣으면 된다.**

## 팀원 설정 (3단계)

1. 학습/변환된 ONNX 파일을 **공유 위치**(드라이브 등)에서 다운로드
2. 이 폴더에 정확한 이름으로 배치:
   ```
   ai-server/models/yolo26-grape.onnx
   ```
3. 빌드 → 이미지에 모델이 포함(bake)되어 동작:
   ```
   docker compose build ai-server
   docker compose up -d ai-server
   ```

## 파일 설명

| 파일 | git 포함 | 설명 |
|---|---|---|
| `yolo26-grape.onnx` | ❌ (다운로드 배치) | YOLO26 추론 모델. 없으면 `docker build` 의 COPY 단계에서 실패 |
| `labels.json` | ✅ | 클래스 인덱스 → 한글명/severity 매핑. **모델 클래스 순서와 일치해야 함**(0=노균병, 1=탄저병) |
| `best.pt` | ❌ | (선택) 변환 입력용 PyTorch 가중치. 런타임 불필요 |

## 참고
- 모델 경로는 `AI_MODEL_PATH`(기본 `models/yolo26-grape.onnx`)로 변경 가능.
- 빌드하지 않고 모델만 교체하려면 볼륨 마운트로 오버라이드 가능:
  `-v ./ai-server/models:/app/models:ro`
- `.pt → .onnx` 변환: `uv sync --group export && uv run yolo export model=models/best.pt format=onnx opset=12 imgsz=640 simplify=True`
