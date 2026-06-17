"""추론 엔드포인트 테스트. 활성 추론기(ONNX/목업) 무관하게 응답 '계약'을 검증한다."""

PREDICT_URL = "/api/v1/disease/predict"
SEVERITIES = {"LOW", "MEDIUM", "HIGH"}


def _post(client, image_bytes, filename="grape-leaf.jpg"):
    return client.post(PREDICT_URL, files={"file": (filename, image_bytes, "image/jpeg")})


def test_predict_returns_contract_fields(client, image_bytes):
    resp = _post(client, image_bytes)
    assert resp.status_code == 200
    body = resp.json()
    # Spring AiAnalysisResponse 계약 7필드(snake_case)
    for field in ("success", "diagnosis", "confidence", "severity",
                  "summary", "recommended_action", "model_version"):
        assert field in body
    assert body["success"] is True
    assert body["severity"] in SEVERITIES
    assert 0.0 <= body["confidence"] <= 1.0


def test_detections_shape_for_client_drawing(client, image_bytes):
    """클라이언트 렌더링용 좌표/클래스 구조 검증(탐지 0건이어도 일관)."""
    body = _post(client, image_bytes).json()
    assert body["detection_count"] == len(body["detections"])
    assert body["image_size"]["width"] > 0 and body["image_size"]["height"] > 0
    for det in body["detections"]:
        assert det["class_name"] and det["label"]
        assert 0.0 <= det["confidence"] <= 1.0
        assert len(det["bbox"]) == 4  # [x, y, width, height] (원본 좌표계)
