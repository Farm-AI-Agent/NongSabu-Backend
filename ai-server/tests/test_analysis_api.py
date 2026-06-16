from io import BytesIO

from fastapi.testclient import TestClient
from PIL import Image

from app.main import app


client = TestClient(app)


def test_health_endpoint_returns_ok():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_predict_endpoint_returns_grape_dummy_result():
    response = client.post(
        "/api/v1/disease/predict",
        files={"file": ("grape-leaf.jpg", image_bytes(), "image/jpeg")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["diagnosis"] == "Grape disease suspicion (dummy)"
    assert body["confidence"] == 0.87
    assert body["severity"] == "LOW"
    assert "grape disease analysis flow" in body["summary"]
    assert "grape-dummy-disease-predictor-v1" in body["model_version"]


def test_predict_endpoint_does_not_branch_by_non_grape_filename():
    response = client.post(
        "/api/v1/disease/predict",
        files={"file": ("tomato-leaf.jpg", image_bytes(), "image/jpeg")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["diagnosis"] == "Grape disease suspicion (dummy)"
    assert body["model_version"].startswith("grape-dummy-disease-predictor-v1")


def image_bytes() -> bytes:
    buffer = BytesIO()
    Image.new("RGB", (4, 3), color="green").save(buffer, format="JPEG")
    return buffer.getvalue()
