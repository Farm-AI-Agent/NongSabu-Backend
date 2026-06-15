from fastapi import FastAPI

from app.api.router import api_router


app = FastAPI(
    title="NongSabu AI Server",
    description="작물 병충해 이미지 분석 전용 FastAPI 서버",
    version="0.1.0",
)

app.include_router(api_router)


@app.get("/", tags=["root"])
def root() -> dict[str, str]:
    return {"message": "NongSabu AI Server is running"}

