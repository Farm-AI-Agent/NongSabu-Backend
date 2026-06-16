"""health / info / root 엔드포인트 테스트."""


def test_root(client):
    assert client.get("/").status_code == 200


def test_health_reports_model_state(client):
    body = client.get("/health").json()
    assert body["status"] == "ok"
    assert "model_loaded" in body
    # 실모델 미연결 → 목업 모드
    assert body["mode"] == "mock"
    assert body["model_loaded"] is False


def test_info_exposes_metadata(client):
    body = client.get("/info").json()
    assert body["mode"] == "mock"
    assert body["model_loaded"] is False
    assert "model_version" in body
