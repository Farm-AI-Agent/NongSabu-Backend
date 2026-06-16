# NongSabu AI Server

포도 병충해 추론 서버. Spring 백엔드가 이미지를 multipart 로 보내면 결과 JSON 을 반환한다.

- 프레임워크: FastAPI (async)
- 가상환경/패키지: **uv** (Python 3.13)
- **현재 동작**: 사전정의 결과를 반환하는 **목업**(`GrapeMockDiseasePredictor`).
  실제 YOLO26 ONNX 추론 코드는 구현돼 있으나 **주석 처리** 상태(모델 학습/변환 후 활성화).
- 작물 게이팅(포도 외 작물 차단)은 **Spring** 이 ai-server 호출 전에 처리한다.

## 개발 (uv)

```bash
cd ai-server
uv sync                       # 목업 실행용 슬림 의존성만 설치 (.venv, Python 3.13)
uv run uvicorn app.main:app --reload --port 8000

uv sync --group dev           # 테스트 도구 포함 설치
uv run pytest                 # 테스트 실행
```

문서: http://localhost:8000/docs · 상태: `/health` · 모델 정보: `/info`

추론 결과 응답 형식(7필드 계약)은 [docs/API.md](docs/API.md) 참고.

## 실모델(ONNX) 활성화 — 학습/변환 완료 후

1. `uv sync --group inference` — numpy, onnxruntime 설치
2. `app/services/disease_predictor.py` 하단의 ONNX 코드 주석 해제 + `build_predictor` 교체
3. `AI_MODEL_PATH` 로 `.onnx` 경로 지정, `labels.json`(클래스 순서) 작성
4. 응답은 동일한 7필드 `AnalysisResponse` 로 매핑(코드 주석 참고)

`.pt -> .onnx` 변환(GPU 서버, 변환 시에만):
```bash
uv sync --group export
uv run yolo export model=yolo26?.pt format=onnx opset=12 imgsz=640 simplify=True
```

## 환경변수

`.env.example` 참고.
