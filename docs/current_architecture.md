# Current Architecture

## 当前唯一主线

- 前端唯一入口：`frontend/src/App.jsx`
- 前端业务主线：`frontend/src/sections/Hero.jsx`、`Upload.jsx`、`Analysis.jsx`、`Generate.jsx`
- 后端主 API：
  - `POST /api/upload/audio`
  - `POST /api/music/recognize`
  - `POST /api/music/features`
  - `POST /api/music/semantic`
  - `POST /api/music/separate`
  - `POST /api/music/intelligence`
  - `POST /api/ai/analyze`
  - `POST /api/ai/generate-image`
  - `GET /api/ai/history`
  - `GET /api/ai/netease`

## 当前运行方式

### 本地开发

- 前端默认通过 Vite proxy 转发 `/api` 到 `http://localhost:8081`
- 后端默认使用 `dev` profile
- 敏感配置全部来自环境变量

### Docker

- Compose 提供 `mysql + redis + backend`
- Stable Diffusion 通过 `APP_SD_ENDPOINT` 指向外部服务
- Python 网易云解析脚本通过 `APP_NETEASE_SCRIPT_PATH` 配置
- 后端容器内的音频分析默认安装 `Essentia`，`/api/music/features` 在 Docker/WSL 场景建议显式使用 `APP_ANALYSIS_PROVIDER=essentia`
- Windows 原生开发继续使用 `APP_ANALYSIS_PROVIDER=auto`，脚本会在检测不到 `essentia` 时自动回退到 `librosa`
- 如果只想单独验证 `Essentia`，优先用 `scripts/run_essentia_docker.py` 和 `scripts/essentia.Dockerfile`

## 关键约束

- 上传成功后返回 `uploadId`，后续识别只接受 `uploadId`
- 上传主线默认会补充音频特征分析与语义分析，AI 分析可读取这些结构化结果
- 兼容期内后端仍接受 `filePath`，但只允许访问已登记且位于上传目录下的文件
- 历史接口返回 `GenerationHistoryItem` DTO，不再返回内部实体
- 消耗型接口可通过 `X-API-Token` 和限流保护

## 当前音频智能分层

- `Demucs`：负责分轨，服务端入口 `POST /api/music/separate`
- `Essentia / librosa`：负责 BPM、调性、和弦、舞动感等结构化特征，入口 `POST /api/music/features`
- `MERT`：负责音乐 embedding 与语义标签，入口 `POST /api/music/semantic`
- `OpenAI` 文本模型：负责将结构化结果转成可读分析和封面 prompt，入口 `POST /api/ai/analyze`
- `POST /api/music/intelligence`：负责把识别、分轨、特征、语义和 GPT 解释串成一步式聚合响应

补充说明：

- 当前项目文档默认按 `OpenAI` 路线描述 LLM 与解释层能力。
- 仓库中仍有少量历史类名保留 `Qwen` 命名，这属于待清理的实现细节，不代表项目当前推荐方案。

## 运行建议

- Windows 本机：继续用现有 `.venv-audio` 和 `provider=auto`
- Windows 本机如果想强制用 `Essentia`：把 `APP_ANALYSIS_SCRIPT_PATH` 切到 `scripts/run_essentia_docker.py`
- Docker/WSL：使用容器内 Python 运行时和 `provider=essentia`
- 如果只想快速验证特征抽取，可以直接运行 `scripts/analyze_audio.py` 对单个音频文件做分析

## 已下线内容

- 旧版前端 `pages/*`
- 未接入主线的旧组件 `UploadSection`、`ResultDisplay`、`MusicPlayer`
- 重复/演示控制器 `FullPipelineController`、`NetEaseController`、`TestController`

## 后续扩展方向

- 拆分 `Track`、`AnalysisResult`、`GenerationRecord`
- 把导入和编排进一步下沉到应用服务层
- 视需要把网易云解析替换为独立服务实现
