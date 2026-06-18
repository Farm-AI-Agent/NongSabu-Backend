"""입력 검증 / 에러 응답 계약 테스트."""

PREDICT_URL = "/api/v1/disease/predict"


def test_non_image_content_type_returns_400(client):
    resp = client.post(PREDICT_URL, files={"file": ("note.txt", b"hello", "text/plain")})
    assert resp.status_code == 400
    body = resp.json()
    assert body["success"] is False
    assert body["error_code"] == "INVALID_IMAGE"
    assert "model_version" in body


def test_corrupt_image_returns_400(client):
    # content-type 은 image 지만 실제 디코딩 불가
    resp = client.post(PREDICT_URL, files={"file": ("bad.png", b"not-an-image", "image/png")})
    assert resp.status_code == 400
    assert resp.json()["error_code"] == "INVALID_IMAGE"


def test_empty_file_returns_400(client):
    resp = client.post(PREDICT_URL, files={"file": ("empty.png", b"", "image/png")})
    assert resp.status_code == 400
    assert resp.json()["error_code"] == "INVALID_IMAGE"


def test_oversized_file_returns_413(client, monkeypatch):
    from app.core.config import get_settings

    # 업로드 한도를 아주 작게 낮춰 413 유도
    settings = get_settings()
    monkeypatch.setattr(settings, "max_upload_mb", 0.0001)
    big = b"\x89PNG" + b"0" * 5000
    resp = client.post(PREDICT_URL, files={"file": ("big.png", big, "image/png")})
    assert resp.status_code == 413
    assert resp.json()["error_code"] == "PAYLOAD_TOO_LARGE"
