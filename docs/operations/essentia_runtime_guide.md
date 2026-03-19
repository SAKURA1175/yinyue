# Essentia 运行指南

## 结论先说

- 当前本地 Windows 开发默认仍是 `librosa`
- `Essentia` 推荐在 `Docker` 或 `WSL2` 里运行
- 不建议依赖原生 Windows 编译 `essentia`

## 当前 provider 规则

- `APP_ANALYSIS_PROVIDER=auto` 时，脚本会自动检测 `essentia`
- 如果当前环境能导入 `essentia`，就走 `essentia`
- 如果不能导入，就自动回退到 `librosa`
- 在你当前这台 Windows 机器上，默认应继续用 `auto`
- 在 Docker / WSL2 环境里，建议显式设成 `essentia`

## Docker 方案

推荐先用独立 helper 验证 `Essentia`，它不依赖原生 Windows 编译，也不要求你先起整套后端。

启动命令：

```powershell
python scripts/run_essentia_docker.py backend/uploads/audio/<your-audio-file>.mp3
```

helper 会使用：

- `APP_ANALYSIS_PROVIDER=essentia`
- `scripts/essentia.Dockerfile`
- `python:3.10-slim-bookworm`
- `Essentia + librosa + soundfile + imageio-ffmpeg`

如果你想把本机 Spring Boot 后端也切到这条 helper 路线，设置下面这些环境变量即可：

```powershell
$env:APP_ANALYSIS_PYTHON_COMMAND="D:\yinyue\.venv-audio\Scripts\python.exe"
$env:APP_ANALYSIS_SCRIPT_PATH="D:\yinyue\scripts\run_essentia_docker.py"
$env:APP_ANALYSIS_PROVIDER="essentia"
$env:APP_ANALYSIS_TIMEOUT_SECONDS="180"
```

这样后端对外还是原来的 `/api/music/features`，只是底层改成容器内 `Essentia`。

如果你要跑完整后端，依然可以使用：

```powershell
docker compose up --build
```

## WSL2 方案

如果你更想在本机 Linux 环境里跑，也可以用 WSL2。建议流程是先装依赖，再建虚拟环境，然后安装项目专用包。

```bash
sudo apt update
sudo apt install -y python3 python3-venv python3-pip ffmpeg build-essential pkg-config \
  libsndfile1 libfftw3-dev libyaml-dev libsamplerate0-dev libtag1-dev libchromaprint-dev \
  libavcodec-dev libavformat-dev libavutil-dev libswresample-dev libswscale-dev

python3 -m venv .venv-audio
source .venv-audio/bin/activate
pip install -U pip setuptools wheel
pip install -r scripts/essentia_requirements.txt
python scripts/analyze_audio.py path/to/audio.mp3 --provider essentia
```

## 本地 Windows 方案

Windows 原生环境继续保留当前的 `librosa` 路线，不强行切换。

```powershell
.\.venv-audio\Scripts\python.exe .\scripts\analyze_audio.py .\backend\uploads\audio\<file>.mp3 --provider auto
```

如果你显式写 `--provider essentia`，但当前环境并没有安装 `essentia`，脚本会失败。这个限制是预期内的。

如果你想在 Windows 本地直接体验 `Essentia`，优先用上面的 Docker helper，而不是原生编译。
