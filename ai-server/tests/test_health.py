"""health / info / root 엔드포인트 테스트."""


def test_root(client):
    assert client.get("/").status_code == 200


def test_health_reports_model_state(client):
    body = client.get("/health").json()
    assert body["status"] == "ok"
    assert "model_loaded" in body
    # 모델 미탑재 환경 → 데모 모드
    assert body["mode"] == "demo"
    assert body["model_loaded"] is False


def test_info_exposes_metadata(client):
    body = client.get("/info").json()
    assert body["mode"] == "demo"
    assert body["classes"] == ["노균병", "탄저병"]
    assert body["input_size"] == 640
    assert "conf_threshold" in body
