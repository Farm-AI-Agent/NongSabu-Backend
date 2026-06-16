"""추론 엔드포인트(목업 기준) 테스트. 기대값은 dev 더미와 동일하게 맞춘다."""

PREDICT_URL = "/api/v1/disease/predict"


def _post(client, image_bytes, filename="grape-leaf.jpg"):
    return client.post(PREDICT_URL, files={"file": (filename, image_bytes, "image/jpeg")})


def test_predict_returns_grape_dummy_result(client, image_bytes):
    resp = _post(client, image_bytes)
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert body["diagnosis"] == "Grape disease suspicion (dummy)"
    assert body["confidence"] == 0.87
    assert body["severity"] == "LOW"


def test_response_contract_fields_present(client, image_bytes):
    """Spring AiAnalysisResponse 계약 7필드가 모두 존재(snake_case)."""
    body = _post(client, image_bytes).json()
    for field in (
        "success", "diagnosis", "confidence", "severity",
        "summary", "recommended_action", "model_version",
    ):
        assert field in body


def test_model_version_includes_image_size(client, image_bytes):
    body = _post(client, image_bytes).json()
    # 목업은 model_version 에 바이트수:너비x높이 를 포함(dev 형식)
    assert body["model_version"].startswith("grape-dummy-disease-predictor-v1:")
