# 项目技术栈深度分析报告

**文档版本**: 1.0  
**生成日期**: 2025-12-13  
**责任人**: AI Assistant

---

## 1. 前端模块 (Frontend Module)

### 1.1 框架体系
- **核心框架**: React v18.2.0 (采用 Functional Components + Hooks)
- **构建工具**: Vite v5.0.8 (提供极速冷启动与 HMR)
- **语言标准**: ECMAScript Modules (ESM)
- **依赖管理**: NPM (Node Package Manager)

### 1.2 组件架构
项目采用基于 **功能域 (Feature-based)** 的目录结构：
- `/src/components/`: 通用 UI 组件 (如 `MusicPlayer`, `ResultDisplay`)，负责展示逻辑，无状态或仅含 UI 状态。
- `/src/sections/`: 页面级区块组件 (如 `Hero`, `Upload`, `Analysis`)，负责具体业务流程的 UI 组合。
- `/src/pages/`: 路由页面组件，作为容器组装 Sections。
- **通信机制**: 
  - **父子组件**: Props 传递。
  - **全局状态**: React Context (主要用于主题或全局配置，若有)。
  - **组件与服务**: 通过 Axios 直接调用后端 API。

### 1.3 状态管理与数据流
- **当前策略**: 主要依赖 React `useState` 和 `useEffect` 进行局部状态管理。
- **数据流向**: 单向数据流 (Parent -> Child)。
- **性能优化**: 
  - Vite 的按需编译。
  - 组件懒加载 (虽未深度检查，但 Vite 默认支持)。

### 1.4 构建与部署
- **开发环境**: `npm run dev` (Vite Dev Server)
- **生产构建**: `npm run build` (输出至 `dist/`)
- **预览**: `npm run preview`
- **环境变量**: 使用 `.env` 文件 (Vite 标准) 管理 API Base URL 等。

### 1.5 质量保障
- **测试**: 目前主要依赖手动测试。建议引入 Vitest + React Testing Library 进行单元测试。
- **类型安全**: 当前使用 JavaScript，建议迁移至 TypeScript 以提升代码健壮性。

---

## 2. 后端模块 (Backend Module)

### 2.1 服务架构
- **框架**: Spring Boot 3.2.0
- **架构模式**: 典型的 MVC 分层架构 (Controller -> Service -> Repository)。
- **运行环境**: Java 17 (LTS)
- **通信协议**: RESTful API (HTTP/1.1), JSON 数据格式。

### 2.2 数据库体系
- **关系型数据库**: MySQL 8.2.0
  - **ORM**: Spring Data JPA (Hibernate 实现)
  - **连接池**: HikariCP (高性能默认池)
  - **策略**: 目前为单库单表模式，适用于中小型应用。
- **缓存数据库**: Redis (Standalone)
  - **客户端**: Jedis
  - **用途**: 潜在的会话管理、热点数据缓存 (如 AI 分析结果缓存)。

### 2.3 接口规范
- **API 风格**: RESTful
- **路径规范**: `/api/v1/...` (建议统一，目前部分为 `/api/...`)
- **JSON 处理**: Google Gson 2.10.1
- **文件处理**: Apache Commons IO 2.11.0

### 2.4 安全机制
- **CORS**: 全局配置允许跨域 (`CorsConfig.java`)，方便前后端分离开发。
- **认证**: 目前未见复杂的 Spring Security 配置，可能依赖简单的 Token 或无状态设计 (待完善)。

### 2.5 监控与日志
- **日志框架**: Log4j2 (通过 Spring Boot Starter)
- **日志级别**: Root: INFO, App: DEBUG (`application.yml` 配置)
- **输出**: 控制台 (Console) 输出。建议生产环境增加文件输出 (File Appender)。

---

## 3. AI 模块 (Artificial Intelligence Module)

### 3.1 算法与模型框架
本系统采用 **混合 AI 架构 (Hybrid AI Architecture)**，结合本地模型与云端 API：

1.  **大语言模型 (LLM)**:
    -   **提供商**: 阿里云 DashScope (通义千问)
    -   **模型**: `qwen-long` (长文本处理能力强)
    -   **用途**: 歌词分析、情感提取、风格建议。
    -   **集成方式**: REST API 调用 (`QwenLLMService.java`)。

2.  **语音识别 (ASR)**:
    -   **提供商**: Audd.io (首选) + 阿里云 (备选 `qwen3-asr-flash`)
    -   **用途**: 音乐指纹识别、歌词转录。
    -   **策略**: 降级策略 (Failover)，Audd 失败自动切换至 Ali Qwen。

3.  **图像生成 (AIGC)**:
    -   **引擎**: Stable Diffusion WebUI (Automatic1111)
    -   **部署**: 本地/独立服务器部署 (API Mode enabled)
    -   **通信**: HTTP API (`/sdapi/v1/txt2img`)
    -   **Prompt 工程**: 由 LLM 分析结果自动生成 Prompt。

### 3.2 数据处理
- **音频处理**: 上传 -> 存储 (本地 `uploads/`) -> 识别。
- **特征工程**: 
  - LLM Prompt 构造: 结合歌名、歌词、风格进行结构化 Prompt 设计。
  - 图像 Prompt 优化: 将自然语言描述转化为 SD 可理解的 Tag 序列 (如 "cyberpunk, neon lights, 8k")。

### 3.3 服务部署
- **策略**: AI 服务作为独立模块或外部依赖，不与主业务逻辑强耦合，通过 HTTP 接口松散连接。
- **稳定性**: 包含重试机制和错误降级 (Fallback) 逻辑。

### 3.4 伦理与合规
- **隐私**: 用户上传音频仅用于分析，不在云端长久留存 (视具体实现而定)。
- **内容审核**: 依赖云端 API (阿里/Audd) 的内置合规过滤。

---

## 4. 总结与建议

### 4.1 优势
- **技术栈现代**: Spring Boot 3 + React 18 是目前主流且稳定的全栈组合。
- **AI 能力丰富**: 整合了听 (ASR)、想 (LLM)、画 (SD) 多模态能力。
- **架构清晰**: 模块职责分明，易于维护。

### 4.2 改进方向
1.  **测试覆盖**: 引入单元测试 (JUnit/Mockito, Vitest) 提升稳定性。
2.  **安全加固**: 引入 Spring Security + JWT 进行用户认证。
3.  **部署容器化**: 编写 Dockerfile 和 docker-compose.yml 实现一键部署。
4.  **文档自动化**: 集成 Swagger/OpenAPI 自动生成接口文档。
