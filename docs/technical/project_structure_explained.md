# 项目包结构与功能详解文档

> 历史说明：本文档包含旧版页面和已下线控制器的描述。当前请优先以 [current_architecture.md](D:/yinyue/docs/current_architecture.md) 为准。

本文档详细解析了 AI 音乐专辑生成项目的代码结构，包括后端（Java Spring Boot）和前端（React）的每一个包/目录的具体内容和主要作用。

## 一、 后端项目结构 (Backend)

后端代码位于 `d:\yinyue\backend\src\main\java\com\yinyue` 目录下，采用标准的 Spring Boot 分层架构。

### 1. `com.yinyue.controller` (控制层)
**作用**：系统的入口，负责接收前端发送的 HTTP 请求，调用 Service 层处理业务，并将结果封装成 JSON 返回给前端。

*   **`AIController.java`**
    *   **内容**：处理与 AI 生成相关的请求。
    *   **核心功能**：接收前端的分析请求，构建 Prompt（提示词），调用 Qwen 大模型进行音乐分析；接收生图请求，调用 Stable Diffusion 生成封面。
*   **`FileUploadController.java`**
    *   **内容**：处理文件上传请求。
    *   **核心功能**：接收用户上传的 MP3 音频文件，将其保存到服务器本地磁盘，并返回访问路径。
*   **`MusicRecognitionController.java`**
    *   **内容**：处理音乐识别请求。
    *   **核心功能**：接收音频文件路径，调用 Audd API 或阿里云 API 识别歌曲信息（歌名、歌手、歌词）。
*   **`NetEaseController.java`**
    *   **内容**：处理网易云音乐链接解析请求。
    *   **核心功能**：调用 Python 脚本 (`netease_parser.py`) 解析网易云链接，提取歌曲元数据。
*   **`FullPipelineController.java`**
    *   **内容**：全流程自动化控制器。
    *   **核心功能**：提供一键式接口，串联上传、识别、分析、绘图四个步骤（目前主要用于测试或高级自动化场景）。
*   **`TestController.java`**
    *   **内容**：测试接口。
    *   **核心功能**：用于开发阶段测试各个模块的连通性。

### 2. `com.yinyue.service` (业务逻辑层)
**作用**：包含核心业务逻辑，协调各个模块（数据库、AI 服务、文件系统）的工作。

*   **`FileUploadService.java`**
    *   **内容**：文件存储逻辑。
    *   **核心功能**：负责将 MultipartFile 文件流写入磁盘，生成唯一文件名，防止重名覆盖。
*   **`MusicTrackService.java`**
    *   **内容**：音乐数据管理逻辑。
    *   **核心功能**：负责将识别到的音乐信息、AI 分析结果、生成的图片路径保存到数据库中，以及查询历史记录。

### 3. `com.yinyue.ai` (AI 集成层)
**作用**：封装第三方 AI 服务的调用逻辑，对内提供统一的接口，屏蔽底层 API 的复杂性。

*   **`llm` 包 (Large Language Model)**
    *   **`QwenLLMService.java`**：封装通义千问（Qwen）大模型的调用。负责发送 Prompt，解析返回的 JSON 格式分析结果。如果调用失败，还负责提供 Mock（模拟）数据进行兜底。
*   **`image` 包 (Image Generation)**
    *   **`StableDiffusionService.java`**：封装 Stable Diffusion WebUI 的 API 调用。负责构建绘图参数（Prompt, Negative Prompt, Sampler, Steps 等），发送绘图请求，并获取 Base64 格式的图片数据。
*   **`music` 包 (Music Recognition)**
    *   **`AuddApiService.java`**：封装 Audd.io 听歌识曲 API。负责发送音频指纹，获取歌曲元数据。
*   **`asr` 包 (Speech Recognition)**
    *   **`QwenAsrService.java`**：(预留) 封装语音转文字服务，用于处理没有歌词的纯音乐或提取人声。

### 4. `com.yinyue.repository` (数据访问层)
**作用**：直接与数据库交互，执行 SQL 语句。

*   **`MusicTrackRepository.java`**
    *   **内容**：继承自 Spring Data JPA 的 `JpaRepository`。
    *   **核心功能**：无需编写 SQL，直接通过方法名（如 `save`, `findById`）对 `music_tracks` 表进行增删改查操作。

### 5. `com.yinyue.entity` (实体层)
**作用**：定义数据库表结构对应的 Java 对象 (POJO)。

*   **`MusicTrack.java`**
    *   **内容**：映射数据库中的 `music_tracks` 表。
    *   **核心功能**：包含 id, title, artist, album, lyrics, aiAnalysisJson, coverImagePath 等字段，是数据传输的载体。

### 6. `com.yinyue.config` (配置层)
**作用**：存放全局配置类。

*   **`CorsConfig.java`**
    *   **内容**：跨域资源共享配置。
    *   **核心功能**：允许前端（localhost:5173）访问后端接口（localhost:8080），解决浏览器的同源策略限制。
*   **`RestTemplateConfig.java`**
    *   **内容**：HTTP 客户端配置。
    *   **核心功能**：配置 `RestTemplate` Bean，用于在 Java 代码中发起 HTTP 请求（调用外部 AI API）。

---

## 二、 前端项目结构 (Frontend)

前端代码位于 `d:\yinyue\frontend\src` 目录下，采用 React + Vite 构建。

### 1. `src/sections` (板块组件)
**作用**：本项目是单页应用 (SPA)，页面被垂直划分为多个全屏板块，每个板块负责一个独立的业务流程。

*   **`Hero.jsx` (首页/英雄区)**
    *   **内容**：落地页展示。
    *   **作用**：展示项目标题、Slogan，提供“网易云链接导入”的入口。
*   **`Upload.jsx` (上传区)**
    *   **内容**：文件上传与识别。
    *   **作用**：提供文件拖拽上传区域，显示网易云解析结果，调用后端上传接口和识别接口。
*   **`Analysis.jsx` (分析区)**
    *   **内容**：AI 分析结果展示。
    *   **作用**：以卡片形式展示 AI 分析出的主题、氛围、风格、色板，以及生成的英文 Prompt。
*   **`Generate.jsx` (生成区)**
    *   **内容**：最终结果展示。
    *   **作用**：展示 Stable Diffusion 生成的最终专辑封面图片。

### 2. `src/components` (通用组件)
**作用**：存放可复用的 UI 小组件。

*   **`Player.jsx`**
    *   **内容**：全局播放器。
    *   **作用**：固定在页面底部的音乐播放条，支持播放/暂停、进度拖拽、时间显示。它接收全局的音频 URL 状态。
*   **`ResultDisplay.jsx`**
    *   **内容**：结果展示组件（旧版）。
    *   **作用**：早期的结果展示组件，目前部分逻辑已迁移至 Sections 中，可作为参考或复用。

### 3. `src/pages` (页面组件 - 备用)
**作用**：如果未来项目扩展为多路由（Multi-page）结构，这里存放完整的页面文件。目前主要逻辑都在 `sections` 中。

*   `Home.jsx`, `Upload.jsx`, `Analyze.jsx`, `Generate.jsx`: 对应的页面级封装。

### 4. 根目录文件
*   **`App.jsx`**
    *   **作用**：前端应用的主入口。负责维护全局状态（如 `musicData` 音乐信息, `audioUrl` 播放地址），并组织各个 `Section` 的渲染顺序。
*   **`main.jsx`**
    *   **作用**：React 的挂载点，将 App 组件渲染到 HTML 的 root 节点上。
*   **`App.css` / `index.css`**
    *   **作用**：全局样式表，定义了字体、颜色变量、基础布局重置等。

---

## 三、 关键工作流 (Workflow)

理解了包结构后，我们可以串联一下一次完整的用户操作在代码中的流转过程：

1.  **上传**:
    *   用户在前端 `Upload.jsx` 拖入文件。
    *   请求发往后端 `FileUploadController` -> `FileUploadService` 保存文件。
    *   前端拿到文件路径，请求 `MusicRecognitionController` -> `AuddApiService` 识别信息。
2.  **分析**:
    *   前端 `App.jsx` 拿到识别结果，自动滚动到 `Analysis.jsx`。
    *   `Analysis.jsx` 加载时请求 `AIController` -> `QwenLLMService`。
    *   `QwenLLMService` 构建 Prompt 发送给阿里云 Qwen，拿到 JSON 分析结果返回前端。
3.  **生图**:
    *   用户在 `Analysis.jsx` 确认无误，点击“生成”。
    *   请求发往 `AIController` -> `StableDiffusionService`。
    *   `StableDiffusionService` 拼接画质修饰词，请求本地 SD WebUI，拿到 Base64 图片返回前端。
    *   前端 `Generate.jsx` 展示最终图片。
