# RAGChecker 평가 데이터 생성

포도 유기재배 매뉴얼에서 근거를 확인한 고정 질문 50개를 사용한다. 스크립트는
NongSabu Spring API에 PDF를 업로드한 뒤 실제 검색 문맥과 RAG 답변을 받아
RAGChecker 입력 JSON을 만든다.

## 실행

먼저 프로젝트 루트에서 서버를 실행한다.

```powershell
docker compose up --build -d
```

RAG 업로드와 검색은 OpenAI embedding을 사용하므로 프로젝트 루트의 `.env`에
`OPENAI_API_KEY`가 필요하다. 답변 생성까지 LLM으로 검증하려면
`APP_LLM_ENABLED=true`를 추가하고, 로컬 검색 흐름만 확인하려면 기본값
`false`를 유지해도 된다.

평가 스크립트 의존성을 설치하고 실행한다.

```powershell
python -m pip install -r scripts/rag_eval/requirements.txt
python scripts/rag_eval/generate_ragchecker_dataset.py
```

기본 출력 파일은
`scripts/rag_eval/output/grape_manual_ragchecker.json`이다.

같은 평가 계정에 PDF를 이미 적재했다면 중복 임베딩을 막기 위해 다음 옵션을
사용한다.

```powershell
python scripts/rag_eval/generate_ragchecker_dataset.py --skip-upload
```

다른 서버나 파일을 사용할 수도 있다.

```powershell
python scripts/rag_eval/generate_ragchecker_dataset.py `
  --base-url http://localhost:8080 `
  --pdf "C:\Users\KDH\Downloads\2019 포도 유기재배 메뉴얼 ebook.pdf" `
  --output "scripts\rag_eval\output\grape_manual_ragchecker.json"
```

생성된 JSON은 RAGChecker의 `RAGResults.from_json(...)` 입력 형식과 호환된다.

## RAGChecker 지표 계산

RAGChecker는 별도 의존성으로 설치한다.

```powershell
python -m pip install -r scripts/rag_eval/requirements-ragchecker.txt
python -m spacy download en_core_web_sm
python scripts/rag_eval/evaluate_with_ragchecker.py
```

프로젝트 루트의 `.env`에 있는 `OPENAI_API_KEY`를 평가 모델 호출에도 사용한다.
기본 평가 모델은 `openai/gpt-4.1-mini`이며 결과는
`scripts/rag_eval/output/grape_manual_ragchecker_scored.json`에 저장된다.

이 실행은 Overall Precision/Recall/F1, Claim Recall, Context Precision,
Context Utilization, Hallucination, Faithfulness를 포함한 RAGChecker 전체 지표를
계산한다. 평가 과정에서도 여러 번의 LLM 호출이 발생하므로 API 비용이 든다.
