FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV CUDA_VISIBLE_DEVICES=""
ENV TRANSFORMERS_NO_TF=1
ENV HF_HUB_DISABLE_TELEMETRY=1

WORKDIR /app

COPY requirements.txt .
RUN python -m pip install --upgrade pip \
    && pip install --no-cache-dir \
        --index-url https://download.pytorch.org/whl/cpu \
        --extra-index-url https://pypi.org/simple \
        torch==2.7.1+cpu \
    && pip install --no-cache-dir -r requirements.txt

COPY app ./app

EXPOSE 8010

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010"]
