# NongSabu AI Server

포도 병충해 추론 서버. Spring 백엔드가 이미지를 multipart 로 보내면 YOLO26(ONNX) 모델로
추론하고 결과 JSON 을 반환한다.

- 프레임워크: FastAPI (async)
- 추론: ONNX Runtime (CPU) — 런타임에 torch / ultralytics 불필요
- 가상환경/패키지: uv

## 개발 (uv)

```bash
cd ai-server
uv sync                       # 런타임 의존성만 설치 (.venv 생성)
uv run uvicorn app.main:app --reload --port 8000

uv sync --group dev           # 테스트 도구 포함 설치
uv run pytest                 # 테스트 실행
```

문서: http://localhost:8000/docs · 상태: `/health` · 모델 정보: `/info`

추론 결과 응답 형식은 [docs/API.md](docs/API.md) 참고.

## 모델

- ai-server 는 **이미 변환된 `.onnx` 모델을 로드해 추론만** 한다.
- 모델 경로는 환경변수 `MODEL_PATH` 로 지정 (미지정/파일 없음 → 더미 추론기로 폴백, 서버는 정상 기동).
- `.pt -> .onnx` 변환은 GPU 서버에서 오프라인 수행:

```bash
uv sync --group export        # ultralytics 포함 설치 (변환 시에만)
uv run yolo export model=yolo26?.pt format=onnx opset=12 imgsz=640 simplify=True
```

## 환경변수

`.env.example` 참고.
