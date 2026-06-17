from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

from dotenv import load_dotenv


def parse_args() -> argparse.Namespace:
    script_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(
        description="생성된 RAG 결과를 RAGChecker 전체 지표로 평가합니다."
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=script_dir / "output" / "grape_manual_ragchecker.json",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=script_dir / "output" / "grape_manual_ragchecker_scored.json",
    )
    parser.add_argument("--extractor-model", default="openai/gpt-4.1-mini")
    parser.add_argument("--checker-model", default="openai/gpt-4.1-mini")
    parser.add_argument("--batch-size-extractor", type=int, default=8)
    parser.add_argument("--batch-size-checker", type=int, default=8)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    project_root = Path(__file__).resolve().parents[2]
    load_dotenv(project_root / ".env")

    if not args.input.is_file():
        print(f"평가 입력 파일을 찾을 수 없습니다: {args.input}", file=sys.stderr)
        return 1
    if not os.getenv("OPENAI_API_KEY"):
        print("OPENAI_API_KEY가 환경 변수 또는 프로젝트 .env에 없습니다.", file=sys.stderr)
        return 1

    try:
        from ragchecker import RAGChecker, RAGResults
        from ragchecker.metrics import all_metrics
    except ImportError:
        print(
            "RAGChecker가 설치되지 않았습니다. "
            "requirements-ragchecker.txt를 먼저 설치하세요.",
            file=sys.stderr,
        )
        return 1

    rag_results = RAGResults.from_json(args.input.read_text(encoding="utf-8"))
    evaluator = RAGChecker(
        extractor_name=args.extractor_model,
        checker_name=args.checker_model,
        batch_size_extractor=args.batch_size_extractor,
        batch_size_checker=args.batch_size_checker,
        openai_api_key=os.environ["OPENAI_API_KEY"],
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    metrics = evaluator.evaluate(
        rag_results,
        metrics=all_metrics,
        save_path=str(args.output),
    )

    print(json.dumps(metrics, ensure_ascii=False, indent=2))
    print(f"상세 평가 결과: {args.output.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
