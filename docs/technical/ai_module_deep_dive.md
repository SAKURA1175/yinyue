# AI 模块代码深度解析 (AI Module Deep Dive)

本文档对 `d:\yinyue\backend\src\main\java\com\yinyue\ai` 包下的所有代码文件进行深度解析。这些文件是连接后端与第三方 AI 服务（阿里云 Qwen、Stable Diffusion、Audd.io）的桥梁。

## 1. 目录结构概览

```
com.yinyue.ai
├── asr
│   └── QwenAsrService.java       // 语音转文字 (ASR) 服务
├── image
│   └── StableDiffusionService.java // AI 绘图服务
├── llm
│   └── QwenLLMService.java       // 大语言模型 (LLM) 服务
└── music
    └── AuddApiService.java       // 音乐识别服务
```

---

## 2. 详细解析

### 2.1 `llm/QwenLLMService.java` (核心大脑)

**作用**：负责与阿里云通义千问（Qwen）大模型进行交互。它的核心任务是“理解音乐”，根据歌词和元数据生成情感分析和绘图提示词。

**核心方法**：

*   **`analyzeMusicLyrics(String lyrics, String title, String artist)`**
    *   **作用**：对外暴露的业务接口。
    *   **逻辑**：它首先调用内部的 `buildMusicAnalysisPrompt` 方法，将歌词、歌名、歌手拼接成一段很长的 Prompt（提示词），告诉 AI：“你是一个音乐分析师，请分析这首歌...”。然后调用 `callQwenLLM` 发送请求。

*   **`callQwenLLM(String prompt)`**
    *   **作用**：底层的 HTTP 请求发送者。
    *   **逻辑**：
        1.  **参数构建**：构建符合 DashScope API 标准的 JSON Body，包含 `model` (模型名), `input` (消息列表), `parameters` (温度系数等)。
        2.  **发送请求**：使用 Spring 的 `RestTemplate` 发送 POST 请求。
        3.  **结果解析**：处理阿里云返回的复杂 JSON，提取出 `output.text` 或 `output.choices[0].message.content` 中的文本内容。
        4.  **容错降级**：如果网络请求失败或 Key 无效，会自动捕获异常并调用 `mockAnalysis` 方法，返回一份预设的“兜底数据”，防止前端页面崩溃。

*   **`mockAnalysis(String prompt)`**
    *   **作用**：生成模拟数据。
    *   **逻辑**：当真实 AI 挂掉时，根据 Prompt 中是否包含“悲伤”、“快乐”等关键词，返回一份静态的 JSON 分析结果。

### 2.2 `image/StableDiffusionService.java` (视觉中枢)

**作用**：负责连接本地运行的 Stable Diffusion WebUI。它将 LLM 生成的文字描述转化为图像。

**核心类与方法**：

*   **`ImageGenerationOptions` (内部静态类)**
    *   **作用**：这是一个**Builder 模式**的配置类。
    *   **解释**：SD 的绘图参数非常多（步数、采样器、CFG Scale、高分修复等），如果直接传参会让方法签名非常长。用 Builder 模式可以链式调用，如 `.width(512).steps(40).build()`，代码更清晰。

*   **`generateAlbumCover(String prompt)`**
    *   **作用**：业务层调用的“傻瓜式”接口。
    *   **逻辑**：
        1.  **Prompt 增强**：它不只是转发你的 Prompt，还会自动加上“魔法词”（如 `masterpiece, best quality, 8k resolution`），让画质瞬间提升。
        2.  **负面提示词 (Negative Prompt)**：自动加上 `nsfw, low quality, bad anatomy` 等词，防止生成畸形或低俗图片。
        3.  **参数锁定**：强制使用 `DPM++ 2M Karras` 采样器和 40 步迭代，保证输出质量。

*   **`generateImage(ImageGenerationOptions options)`**
    *   **作用**：底层的 HTTP 请求发送者。
    *   **逻辑**：将参数对象转为 JSON，POST 发送到 `http://127.0.0.1:7860/sdapi/v1/txt2img`，收到响应后提取 Base64 图片字符串。

### 2.3 `music/AuddApiService.java` (听歌识曲)

**作用**：负责调用 Audd.io 的 API，识别上传的音频文件是什么歌。

**核心方法**：

*   **`recognizeAudio(File audioFile)`**
    *   **作用**：文件流识别模式。
    *   **逻辑**：将本地 MP3 文件读取为 `byte[]` 数组，再转为 Base64 字符串（这是 Audd API 的要求），放入 `audio` 字段发送请求。

*   **`recognizeAudioUrl(String audioUrl)`**
    *   **作用**：URL 识别模式。
    *   **逻辑**：直接将音频的 URL 发给 Audd，让 Audd 的服务器自己去下载分析。这种方式比上传文件快得多，但要求 URL 必须是公网可访问的。

*   **`parseResult(String resultJson)`**
    *   **作用**：数据清洗。
    *   **逻辑**：Audd 返回的 JSON 包含很多无关信息。这个方法只提取我们关心的 `title`, `artist`, `album`, `lyrics`，封装成 `MusicInfo` 对象。

### 2.4 `asr/QwenAsrService.java` (语音转写)

**作用**：调用阿里云的语音识别模型（Qwen-Audio 或 Paraformer），将音频转为文字。

**核心方法**：

*   **`transcribeAudio(File audioFile)`**
    *   **逻辑**：
        1.  读取音频文件并转 Base64。
        2.  构建请求体，指定 `format` (文件格式，如 wav/mp3) 和 `language` (zh)。
        3.  发送到阿里云 DashScope ASR 接口。
        4.  返回识别出的文本。
    *   **用途**：主要用于那些没有歌词的纯音乐，或者用户上传的是一段哼唱/录音，系统可以通过它提取内容辅助分析。

## 3. 关键技术点总结

1.  **RestTemplate**: Spring 提供的同步 HTTP 客户端，用于后端发送网络请求。
2.  **Gson**: Google 的 JSON 处理库，用于对象与 JSON 字符串的互转。
3.  **Base64 编码**: 图片和音频文件在网络传输时，通常需要转为 Base64 字符串。
4.  **Builder Pattern**: 在 `StableDiffusionService` 中使用，解决多参数构造复杂对象的问题。
5.  **Failover (容错降级)**: 在 `QwenLLMService` 中，当 API 调用失败时切换到 Mock 数据，这是生产级系统的必备特性。
