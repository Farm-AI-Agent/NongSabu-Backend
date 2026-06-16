"""추론 엔드포인트(데모 더미 기준) 테스트."""

PREDICT_URL = "/api/v1/disease/predict"


def _post(client, image_bytes, filename):
    return client.post(
        PREDICT_URL, files={"file": (filename, image_bytes, "image/png")}
    )


def test_healthy_returns_normal_no_detections(client, image_bytes):
    resp = _post(client, image_bytes, "grape_healthy.png")
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert body["diagnosis"] == "정상"
    assert body["severity"] == "LOW"
    assert body["detection_count"] == 0
    assert body["detections"] == []


def test_downy_mildew_detected(client, image_bytes):
    body = _post(client, image_bytes, "grape_downy.png").json()
    assert body["diagnosis"] == "노균병"
    assert body["severity"] == "HIGH"
    assert body["detection_count"] == 1
    det = body["detections"][0]
    assert det["class_name"] == "downy_mildew"
    assert len(det["bbox"]) == 4
    assert 0.0 <= det["confidence"] <= 1.0


def test_anthracnose_detected(client, image_bytes):
    body = _post(client, image_bytes, "grape_anthracnose.png").json()
    assert body["diagnosis"] == "탄저병"
    assert body["severity"] == "MEDIUM"
    assert body["detection_count"] == 1


def test_response_contract_fields_present(client, image_bytes):
    """Spring AiAnalysisResponse 계약 필드가 모두 존재(snake_case)."""
    body = _post(client, image_bytes, "grape_downy.png").json()
    for field in (
        "success", "diagnosis", "confidence", "severity",
        "summary", "recommended_action", "model_version",
    ):
        assert field in body
