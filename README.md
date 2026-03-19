# Yinyue

AI 音乐分析与专辑封面生成项目，当前已收敛为一条可维护主线：

- 前端：`frontend/src/sections`
- 后端：`/api/upload`、`/api/music/recognize`、`/api/music/features`、`/api/music/semantic`、`/api/music/separate`、`/api/music/intelligence`、`/api/ai/analyze`、`/api/ai/generate-image`、`/api/ai/history`、`/api/ai/netease`

## 快速开始

### 本地开发
0. 复制后端/根环境示例：参考 [.env.example](.env.example)
1. 复制前端环境示例：参考 [frontend/.env.example](frontend/.env.example)
2. 配置后端环境变量：
   - `APP_DB_URL`
   - `APP_DB_USERNAME`
   - `APP_DB_PASSWORD`
   - `APP_LLM_API_KEY`
   - `APP_LLM_BASE_URL`
   - `APP_LLM_MODEL`
   - `APP_AUDD_API_KEY`
   - `APP_SD_ENDPOINT`
   - 当前文档口径统一按 `OpenAI` 路线说明，优先使用 `APP_LLM_*`
   - 仓库里仍有少量历史 `Qwen` 命名的类或兼容变量，它们应视为待收口的历史实现，不再作为推荐配置入口
   - 后端会自动读取 `backend/.env` 和项目根目录 `.env`
3. 启动后端：
   - 推荐 JDK 17+
   - `cd backend && mvn spring-boot:run`
4. 启动前端：
   - `cd frontend && npm install && npm run dev`

### Docker Compose
1. 准备环境变量：
   - `APP_LLM_API_KEY`
   - `APP_LLM_BASE_URL`
   - `APP_LLM_MODEL`
   - `APP_AUDD_API_KEY`
   - `APP_API_TOKEN`（必填，用于保护写接口）
   - 可选：`APP_SD_ENDPOINT`
   - 可选：`APP_ANALYSIS_PROVIDER`，默认 `essentia`
   - 可直接从 [.env.example](.env.example) 复制一份开始
2. 启动：
   - `docker compose up --build`

默认 Compose 会启动 `mysql + redis + backend`，Stable Diffusion 作为外部 HTTP 服务接入。
后端容器会把音频分析切到 `Essentia`，本地 Windows 仍保留 `librosa` 退路。

### Essentia Helper
如果你只想先验证 `Essentia` 路线，推荐直接用这个独立 helper：

```powershell
python scripts/run_essentia_docker.py backend/uploads/audio/<file>.mp3
```

它会自动：

- 使用固定在 `bookworm` 的轻量 Python 镜像构建 helper
- 安装 `Essentia + librosa + soundfile + imageio-ffmpeg`
- 以 `--provider essentia` 运行 `scripts/analyze_audio.py`

### Essentia / WSL
如果你不想依赖原生 Windows 编译，推荐直接看：

- [Essentia 运行指南](docs/operations/essentia_runtime_guide.md)

如果你在 Windows 本地开发，又想直接用 `Essentia`，现在也可以不装原生 C++ 环境，直接把后端分析脚本切到 `scripts/run_essentia_docker.py`，让它调用 Docker 容器里的 `Essentia`。

## 当前约定

- 上传接口返回 `uploadId`，前端不再依赖服务器本地文件路径。
- 主线音频增强能力已接入：Demucs 分轨、Essentia/librosa 特征分析、MERT 语义表征。
- 新增一步式聚合接口 `/api/music/intelligence`，可直接返回识别结果、结构化特征、语义标签和 GPT 解释。
- 当前项目规划中的默认解释层是 `OpenAI` 文本模型，不再以 `Qwen` 作为文档默认方案。
- 历史接口返回 DTO，不再直接暴露内部实体和文件路径。
- 运行时目录、构建产物、上传文件默认写到 `runtime/` 或被 `.gitignore` 排除。

## 文档

- 当前架构与启动说明：[docs/current_architecture.md](docs/current_architecture.md)
- 历史文档索引：[docs/README.md](docs/README.md)
