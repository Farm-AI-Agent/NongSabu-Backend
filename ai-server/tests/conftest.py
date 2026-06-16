from io import BytesIO

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.main import app


@pytest.fixture
def client():
    # with 컨텍스트로 lifespan(모델 로드/limiter 생성)을 실행한다.
    with TestClient(app) as c:
        yield c


def make_image_bytes(size=(640, 480), color="green") -> bytes:
    buf = BytesIO()
    Image.new("RGB", size, color).save(buf, "PNG")
    return buf.getvalue()


@pytest.fixture
def image_bytes():
    return make_image_bytes()
