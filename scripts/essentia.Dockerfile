FROM python:3.10-slim-bookworm

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /work

COPY scripts/essentia_requirements.txt /work/scripts/essentia_requirements.txt

RUN pip install --no-cache-dir \
    -r /work/scripts/essentia_requirements.txt

RUN python -c "import imageio_ffmpeg, os; target='/usr/local/bin/ffmpeg'; os.path.lexists(target) and os.unlink(target); os.symlink(imageio_ffmpeg.get_ffmpeg_exe(), target)"

ENTRYPOINT ["python", "/work/scripts/analyze_audio.py", "--provider", "essentia"]
