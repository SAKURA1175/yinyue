# 历史说明

本文档包含较多旧版环境准备和目录示例，当前主线请优先参考 `docs/current_architecture.md` 与根目录 `README.md`。

# 🎵 零基础全栈开发与部署手册：从零打造 AI 音乐专辑生成器

**版本**: 1.0  
**适用人群**: 编程零基础或初学者  
**目标**: 在一台全新的 Windows 电脑上，从安装第一个软件开始，直到成功运行本项目的完整指南。

---

## 📖 目录

- [🎵 零基础全栈开发与部署手册：从零打造 AI 音乐专辑生成器](#-零基础全栈开发与部署手册从零打造-ai-音乐专辑生成器)
  - [📖 目录](#-目录)
  - [1. 项目简介](#1-项目简介)
  - [2. 第一阶段：环境搭建](#2-第一阶段环境搭建)
    - [2.1 基础工具](#21-基础工具)
    - [2.2 后端环境 (Java)](#22-后端环境-java)
    - [2.3 前端环境 (Node.js)](#23-前端环境-nodejs)
    - [2.4 数据库](#24-数据库)
    - [2.5 Python (AI 运行环境)](#25-python-ai-运行环境)
  - [3. 第二阶段：资源与账号准备](#3-第二阶段资源与账号准备)
  - [4. 第三阶段：AI 绘画服务 (Stable Diffusion) 搭建](#4-第三阶段ai-绘画服务-stable-diffusion-搭建)
  - [5. 第四阶段：后端开发环境配置与运行](#5-第四阶段后端开发环境配置与运行)
  - [6. 第五阶段：前端开发环境配置与运行](#6-第五阶段前端开发环境配置与运行)
  - [7. 第六阶段：联调与最终运行](#7-第六阶段联调与最终运行)
  - [8. 常见问题 (FAQ)](#8-常见问题-faq)

---

## 1. 项目简介

我们要搭建的是一个 **AI 音乐专辑生成网站**。
*   **功能**: 用户上传一段音乐 -> AI 听懂歌词和情感 -> AI 自动画出一张专辑封面 -> 生成网页展示。
*   **技术栈** (不用怕，后面会一个个装):
    *   **前端 (用户看到的界面)**: React + Vite
    *   **后端 (处理逻辑的大脑)**: Java (Spring Boot)
    *   **数据库 (存数据的仓库)**: MySQL + Redis
    *   **AI (画画和听歌)**: Stable Diffusion + 阿里通义千问

---

## 2. 第一阶段：环境搭建

请按顺序下载并安装下列软件。**安装路径尽量不要包含中文或空格**。

### 2.1 基础工具
1.  **Git (代码下载工具)**
    *   下载: [https://git-scm.com/download/win](https://git-scm.com/download/win)
    *   安装: 一路点击 "Next" 直到完成。
    *   验证: 打开 CMD (Win+R 输入 `cmd`), 输入 `git --version`，显示版本号即成功。

2.  **VS Code (写代码的工具)**
    *   下载: [https://code.visualstudio.com/](https://code.visualstudio.com/)
    *   安装: 默认安装即可。

### 2.2 后端环境 (Java)
1.  **JDK 17 (Java 开发工具包)**
    *   下载: [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/#java17) (选择 Windows x64 Installer)
    *   安装: 记住安装路径 (例如 `C:\Program Files\Java\jdk-17`).
    *   **关键步骤 (配置环境变量)**:
        *   右键"此电脑" -> 属性 -> 高级系统设置 -> 环境变量。
        *   在"系统变量"新建 -> 变量名: `JAVA_HOME`, 变量值: `你的安装路径`。
        *   在"Path"变量中添加: `%JAVA_HOME%\bin`。
    *   验证: CMD 输入 `java -version`。

2.  **Maven (Java 项目管理工具)**
    *   下载: [Apache Maven](https://maven.apache.org/download.cgi) (下载 Binary zip archive)
    *   安装: 解压到非中文目录 (如 `D:\maven`).
    *   配置: 把 `D:\maven\bin` 添加到系统的 "Path" 环境变量中。
    *   验证: CMD 输入 `mvn -v`。

### 2.3 前端环境 (Node.js)
1.  **Node.js (运行前端的环境)**
    *   下载: [Node.js LTS 版本](https://nodejs.org/) (目前推荐 v18 或 v20)
    *   安装: 一路 Next。
    *   验证: CMD 输入 `node -v` 和 `npm -v`。

### 2.4 数据库
1.  **MySQL (存数据)**
    *   下载: [MySQL Installer](https://dev.mysql.com/downloads/installer/)
    *   安装: 选择 "Server only" 即可。
    *   **重要配置**: 设置 root 密码为 `1234` (为了匹配本项目默认配置)。
    *   验证: 打开 "MySQL Command Line Client", 输入密码 `1234` 能进入。

2.  **Redis (存缓存)**
    *   下载: [Redis for Windows](https://github.com/tporadowski/redis/releases) (下载 .zip)
    *   安装: 解压即可。
    *   运行: 双击 `redis-server.exe`，看到一个黑框框跳出来不要关。

### 2.5 Python (AI 运行环境)
1.  **Python 3.10 (专门用于 Stable Diffusion)**
    *   下载: [Python 3.10.x](https://www.python.org/downloads/release/python-31011/)
    *   安装: **务必勾选 "Add Python to PATH"**。
    *   验证: CMD 输入 `python --version`。

---

## 3. 第二阶段：资源与账号准备

本项目需要用到云端 API，需要去官网申请 Key。

1.  **阿里云 DashScope (通义千问)**
    *   网址: [https://dashscope.console.aliyun.com/](https://dashscope.console.aliyun.com/)
    *   操作: 注册 -> 开通 DashScope -> 创建 API-KEY。
    *   记录: 获得 `sk-xxxxxxxx` 格式的密钥。

2.  **Audd.io (音乐识别)**
    *   网址: [https://audd.io/](https://audd.io/)
    *   操作: 注册 -> Dashboard -> 复制 API Token。

---

## 4. 第三阶段：AI 绘画服务 (Stable Diffusion) 搭建

这是最复杂的一步，请耐心。

1.  **下载 Stable Diffusion WebUI**
    *   在 D 盘创建一个文件夹 `ai_draw`。
    *   进入文件夹，右键 -> Open Git Bash -> 输入:
        ```bash
        git clone https://github.com/AUTOMATIC1111/stable-diffusion-webui.git
        ```

2.  **配置启动参数**
    *   进入 `stable-diffusion-webui` 文件夹。
    *   找到 `webui-user.bat`，右键 -> 编辑。
    *   修改 `set COMMANDLINE_ARGS=` 这一行，改为:
        ```bat
        set COMMANDLINE_ARGS=--api --listen --xformers
        ```
        *(解释: --api 允许后端调用，--listen 允许远程访问)*

3.  **首次启动**
    *   双击 `webui-user.bat`。
    *   **等待**: 第一次启动会自动下载几个 G 的依赖和模型，可能需要 10-30 分钟。
    *   **成功标志**: 看到 `Running on local URL:  http://0.0.0.0:7860`。

---

## 5. 第四阶段：后端开发环境配置与运行

1.  **获取代码**
    *   假设项目代码在 `D:\yinyue`。

2.  **初始化数据库**
    *   打开 MySQL 客户端或 Workbench。
    *   执行 SQL 命令:
        ```sql
        CREATE DATABASE yinyue_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
        ```
    *   本项目启动时会自动创建表，无需手动建表。

3.  **配置后端**
    *   用 VS Code 打开 `D:\yinyue\backend`。
    *   找到 `src/main/resources/application.yml`。
    *   **填空题**:
        *   找到 `qwen: api-key:` -> 填入你的阿里云 Key。
        *   找到 `audd: api-key:` -> 填入你的 Audd Key。
        *   检查数据库密码是否为 `1234`。

4.  **运行后端**
    *   在 VS Code 中打开终端 (Terminal -> New Terminal)。
    *   输入命令打包: `mvn clean package -DskipTests`
    *   看到 "BUILD SUCCESS" 后，运行:
        ```bash
        java -jar target/yinyue-backend-1.0.0.jar
        ```
    *   **成功标志**: 看到日志滚动，最后显示 `Started YinyueApplication in ... seconds`。

---

## 6. 第五阶段：前端开发环境配置与运行

1.  **安装依赖**
    *   用 VS Code 打开 `D:\yinyue\frontend`。
    *   打开终端，输入:
        ```bash
        npm install
        ```
    *   等待进度条跑完。

2.  **启动前端**
    *   输入命令:
        ```bash
        npm run dev
        ```
    *   **成功标志**: 看到 `Local: http://localhost:5173/`。

---

## 7. 第六阶段：联调与最终运行

现在你开启了三个黑色窗口 (Terminal):
1.  **Redis 窗口**: 默默运行中。
2.  **SD WebUI 窗口**: 显示 Model loaded。
3.  **后端窗口**: 显示 Started YinyueApplication。
4.  **前端窗口**: 显示 Local URL。

**开始使用**:
1.  打开浏览器，访问 `http://localhost:5173`。
    *   如果看到紫色的 "AI 音乐专辑生成器" 界面，恭喜你，前端好了！
2.  点击 "点击上传音乐"。
    *   选择一个 `.mp3` 文件。
3.  **观察魔法**:
    *   上传进度条走完 -> 后端日志显示 "File uploaded"。
    *   界面显示 "正在识别歌曲..." -> 后端日志调用 Audd/Ali API。
    *   界面显示 "正在分析歌词..." -> 后端日志调用 Qwen LLM。
    *   界面显示 "正在生成封面..." -> SD WebUI 窗口开始有进度条滚动。
4.  **结果**:
    *   几秒后，一张独一无二的专辑封面出现在网页上。

---

## 8. 常见问题 (FAQ)

**Q: 启动后端报错 "Address already in use: bind"?**
A: 端口被占用了。可能是你启动了两次后端。打开任务管理器，结束所有 `java.exe` 进程，重新运行。

**Q: 前端显示 "Network Error"?**
A: 后端没启动，或者后端报错了。检查后端那个黑窗口有没有红色的错误信息。

**Q: AI 生成图片一直是全黑的或者报错?**
A: 检查 `webui-user.bat` 里有没有加 `--api`。检查 Stable Diffusion 是否真的启动成功了 (浏览器访问 http://127.0.0.1:7860 看看能不能打开)。

**Q: 阿里云 API 报错?**
A: 检查 `application.yml` 里的 Key 是不是复制错了，或者余额不足。

---

**结语**: 
全栈开发并不神秘，无非就是搭积木。你现在已经成功把这四块积木（前、后、库、AI）搭在了一起。继续探索代码吧！
