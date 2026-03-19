FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -q -DskipTests dependency:go-offline

COPY backend backend
RUN mvn -f backend/pom.xml -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 \
        python3-dev \
        python3-pip \
        python3-venv \
        build-essential \
        pkg-config \
        ffmpeg \
        libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY scripts /app/scripts
COPY --from=build /workspace/backend/target/yinyue-backend-1.0.0.jar /app/app.jar
COPY netease_parser.py /app/netease_parser.py

RUN python3 -m venv /opt/audio-venv \
    && /opt/audio-venv/bin/pip install -U pip setuptools wheel \
    && /opt/audio-venv/bin/pip install --prefer-binary -r /app/scripts/essentia_requirements.txt

ENV APP_PYTHON_COMMAND=/opt/audio-venv/bin/python \
    APP_ANALYSIS_PYTHON_COMMAND=/opt/audio-venv/bin/python \
    APP_ANALYSIS_SCRIPT_PATH=/app/scripts/analyze_audio.py \
    APP_ANALYSIS_PROVIDER=essentia \
    APP_SEMANTIC_PYTHON_COMMAND=/opt/audio-venv/bin/python \
    APP_SEMANTIC_SCRIPT_PATH=/app/scripts/extract_music_semantics.py \
    APP_SEPARATION_PYTHON_COMMAND=/opt/audio-venv/bin/python \
    APP_SEPARATION_SCRIPT_PATH=/app/scripts/separate_audio.py

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
