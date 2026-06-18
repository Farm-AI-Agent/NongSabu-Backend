from io import BytesIO

from fastapi.testclient import TestClient
from PIL import Image

from app.main import app


def test_health_endpoint_returns_ok():
    with TestClient(app) as client:
        response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["model_loaded"] is True
    assert body["mode"] == "onnx"


def test_predict_endpoint_returns_yolo_contract():
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/disease/predict",
            files={"file": ("grape-leaf.jpg", image_bytes(), "image/jpeg")},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["model_version"] == "yolo26-grape-onnx-v1"
    assert "dummy" not in body["diagnosis"].lower()
    assert 0.0 <= body["confidence"] <= 1.0
    assert body["detection_count"] == len(body["detections"])
    assert body["image_size"] == {"width": 640, "height": 480}
    for detection in body["detections"]:
        assert detection["label"]
        assert detection["confidence_percent"] == round(detection["confidence"] * 100.0, 1)
        assert len(detection["bbox"]) == 4


def test_predict_endpoint_does_not_branch_by_non_grape_filename():
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/disease/predict",
            files={"file": ("tomato-leaf.jpg", image_bytes(), "image/jpeg")},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["model_version"] == "yolo26-grape-onnx-v1"


def image_bytes() -> bytes:
    buffer = BytesIO()
    Image.new("RGB", (640, 480), color="green").save(buffer, format="JPEG")
    return buffer.getvalue()
