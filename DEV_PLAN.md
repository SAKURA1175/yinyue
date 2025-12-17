# 📅 AI 音乐专辑项目 - 开发进度与执行计划

## 📊 1. 项目当前状态 (Project Status)

**总体进度**: 🟢 **80% (后端与 SD 核心集成完成，前端已对接，数据持久化已实现)**

| 模块 | 功能点 | 状态 | 说明 |
| :--- | :--- | :--- | :--- |
| **后端架构** | Spring Boot 基础搭建 | ✅ 完成 | 运行在 8080 端口 |
| | 数据库连接 | ✅ 完成 | 实体类与 Service 已实现保存逻辑 |
| **核心业务** | 音乐识别 (AudD) | ✅ 完成 | 接口 `/api/music/recognize` 已验证 |
| | 歌词分析 (Qwen LLM) | ✅ 完成 | 接口 `/api/ai/analyze` 已验证 (Mock模式) |
| | 图像生成 (SD + Hires) | ✅ 完成 | 接口 `/api/ai/generate-image` 已验证 (集成 SD API) |
| | 网易云解析 | ✅ 完成 | 重构为 Python 脚本模式，接口 `/api/ai/netease/parse` 已验证 |
| **前端架构** | React + Vite 环境 | ✅ 完成 | 运行在 5173 端口 |
| | 页面结构 | ✅ 完成 | 基础页面与分析结果展示已集成 |
| | 播放器组件 | ✅ 完成 | 基础 UI 已实现 |
| **测试** | 接口验证脚本 | ✅ 完成 | `verify_ai_module.py` 可进行端到端测试 |

---

## 🗓️ 2. 详细执行计划 (Action Plan)

### 🚀 阶段一：数据闭环与历史记录 (当前重点)
**目标**：让用户生成的内容能保存下来，刷新页面不丢失。

- [ ] **1.1 完善数据持久化**
    - 修改 `AIController`，在生成图片成功后，将 (Prompt, Base64图片, 参数) 保存到 MySQL。
    - 修改 `MusicRecognitionController`，在识别成功后，保存音乐元数据。
    - 实现将 Base64 图片转存为本地文件（`/uploads/images/`），数据库只存路径。

- [ ] **1.2 历史记录接口**
    - 新增 `GET /api/history` 接口，分页返回用户生成的作品列表。
    - 新增 `GET /api/history/{id}` 接口，查看详情。

### 🎨 阶段二：前端深度对接
**目标**：前端页面真实可用，不再使用 Mock 数据。

- [ ] **2.1 网易云解析对接**
    - 前端“输入链接”组件调用 `/api/ai/netease/parse`。
    - 解析成功后自动填充“歌曲信息”表单。

- [ ] **2.2 AI 生成对接**
    - 前端“生成封面”按钮调用 `/api/ai/generate-image`。
    - 增加 Loading 状态（预计 5-10秒）。
    - 成功后展示图片，并刷新“我的作品”列表。

- [ ] **2.3 历史页面开发**
    - 开发 `/gallery` 或 `/history` 页面，网格展示所有生成的封面。

### 🌐 阶段三：部署与优化
**目标**：达到上线标准。

- [ ] **3.1 部署脚本**
    - 编写 `Dockerfile` 或一键启动脚本（整合 Python 环境、Java 环境、前端构建）。
- [ ] **3.2 体验优化**
    - 引入 Redis 缓存网易云解析结果。
    - 优化图片加载速度（缩略图）。

---

## 🛠️ 3. 数据库设计 (当前简版)

```sql
CREATE TABLE music_track (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    album VARCHAR(255),
    file_path VARCHAR(500), -- 音频文件路径
    cover_url VARCHAR(500), -- 原封面 URL
    
    -- AI 生成结果
    ai_analysis TEXT,       -- JSON 格式的分析结果
    generated_cover_path VARCHAR(500), -- 生成的图片本地路径
    prompt_used TEXT,       -- 生成时使用的 Prompt
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50)      -- PENDING, COMPLETED, FAILED
);
```

## 📝 4. 下一步行动 (Next Step)
**立即执行**：修改后端代码，实现数据保存逻辑，确保生成的每一张图片都有迹可循。
