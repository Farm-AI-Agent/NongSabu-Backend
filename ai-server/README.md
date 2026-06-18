# NongSabu AI Server

포도 병충해 추론 서버. 클라이언트(또는 Spring)가 이미지를 multipart 로 보내면 **YOLO26 ONNX** 로
추론해 결과(bbox + 병명) JSON 을 반환한다.

- 프레임워크: FastAPI (async) · 패키지/런타임: **uv** (Python 3.13)
- 추론: **ONNX Runtime**. `AI_DEVICE=auto` 면 GPU 가능 시 CUDA, 아니면 CPU.
- 모델: 포도 전용(노균병/탄저병). 탐지 0건 = 정상. (포도 외 작물 차단은 Spring 작물 게이팅 담당)
- 설정은 비밀이 없어 **`config.py` 기본값으로 동작**(.env 불필요, 오버라이드 시에만 사용).

## 모델 배치 (팀원 필독)

모델 가중치(`*.onnx`)는 용량 때문에 **git 에 올리지 않는다.** `labels.json` 은 git 에 포함.
→ 팀원은 **onnx 파일만 받아** `ai-server/models/yolo26-grape.onnx` 에 두면 된다.
자세한 건 [models/README.md](models/README.md).

```
ai-server/models/
├── yolo26-grape.onnx   ← 다운로드해서 배치 (git 미포함)
└── labels.json         ← git 포함 (클래스 매핑)
```

## 실행 (로컬, uv)

```bash
cd ai-server
uv sync --group inference          # onnxruntime/numpy 포함 설치 (.venv, Python 3.13)
uv run uvicorn app.main:app --reload --port 8000

uv sync --group dev                # 테스트 도구
uv run pytest
```

문서: http://localhost:8000/docs · 상태: `/health` · 모델정보: `/info`
응답 형식(7필드 + detections/bbox): [docs/API.md](docs/API.md)

## 실행 (Docker)

```bash
docker build -t nongsabu-ai-server ./ai-server   # 모델을 이미지에 포함(bake) — 빌드 시 models/ 에 onnx 필요
docker run -d -p 8000:8000 nongsabu-ai-server     # .env/볼륨 없이 동작(자체완결)
```
- 모델은 Dockerfile 에서 `COPY models/*.onnx` 로 **이미지에 구워짐**. 교체 = 재빌드.
- 개발 중 빌드 없이 교체하려면 볼륨 오버라이드: `-v ./ai-server/models:/app/models:ro`
- GPU: `Dockerfile.gpu`(onnxruntime-gpu) + `--gpus all`.

## 설정 (env, 전부 선택사항 — 비밀 아님)

`.env.example` 참고. 기본값은 `config.py`. 주요 키:
`AI_MODEL_PATH`(기본 models/yolo26-grape.onnx) · `AI_CONF_THRESHOLD`(0.25) · `AI_NMS_FREE`(자동) ·
`AI_DEVICE`(auto) · `AI_SAVE_ANNOTATED`(검증용 입력+bbox 저장) · `AI_MAX_UPLOAD_MB`.

## 참고
- 모델 학습 전 통신검증용 **목업**(`GrapeMockDiseasePredictor`)은 `disease_predictor.py` 하단에 주석 보존.
- `.pt → .onnx` 변환: `uv sync --group export && uv run yolo export model=models/best.pt format=onnx opset=12 imgsz=640 simplify=True`
- 검증 가이드: [docs/curl-ai-server.md](docs/curl-ai-server.md) · 연동 협의: [docs/INTEGRATION.md](docs/INTEGRATION.md)
