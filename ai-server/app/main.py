from fastapi import FastAPI

from app.api.router import api_router


app = FastAPI(
    title="NongSabu AI Server",
    description="Crop disease prediction server for NongSabu backend",
    version="0.1.0",
)

app.include_router(api_router)


@app.get("/", tags=["root"])
def root() -> dict[str, str]:
    return {"message": "NongSabu AI Server is running"}
