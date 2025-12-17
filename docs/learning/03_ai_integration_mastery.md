# 🤖 03. AI 集成：指挥数字员工

本项目最酷的部分就是 AI。但从代码角度看，**AI 只是一个远程的函数调用**。
你给它发一段 JSON（问题），它回你一段 JSON（答案）。

---

## 1. LLM 调用 (通义千问)

**核心文件**: `backend/.../QwenLLMService.java`

**原理**：
我们并没有在本地运行一个大脑，而是通过 **HTTP 请求** 访问阿里云的服务器。

**步骤拆解**：
1.  **构造 Prompt (提示词)**:
    *   代码里并没有直接问 "分析这首歌"，而是写了一段很长的 Prompt：
    *   `"你是一个专业的音乐评论家...请从以下歌词中分析情感...并以 JSON 格式返回..."`
    *   **Prompt Engineering (提示词工程)** 是 AI 开发的核心。你必须把 AI 当作一个很聪明但需要明确指令的实习生。

2.  **发送请求**:
    *   使用 `RestTemplate` 发送 POST 请求到阿里云 API 地址。
    *   Header 里带上 `Authorization: Bearer sk-xxxx` (你的身份证)。
    *   Body 里带上 `{"model": "qwen-long", "messages": [...]}`。

3.  **解析响应**:
    *   阿里云返回一大坨 JSON。
    *   我们用 Gson 库或者手动解析，提取出 `content` 字段里的文字。

---

## 2. AIGC 调用 (Stable Diffusion)

**核心文件**: `backend/.../StableDiffusionService.java`

**原理**：
Stable Diffusion 本质上是一个运行在本地（终端7）的 Web 服务器。它也有 API。

**流程**：
1.  **文生图接口**: `/sdapi/v1/txt2img`。
2.  **Payload (载荷)**:
    ```json
    {
      "prompt": "cyberpunk city, neon lights, masterpiece", // 正向提示词
      "negative_prompt": "ugly, blurry", // 反向提示词（不想要的）
      "steps": 20, // 画多少步
      "width": 512,
      "height": 512
    }
    ```
3.  **Base64 解码**:
    *   SD 返回的不是一张 `.jpg` 文件，而是一长串乱码一样的字符串 (Base64)。
    *   前端 `src/App.jsx` 里直接用 `<img src={"data:image/png;base64," + imageBase64} />` 就能把它显示成图片，不需要保存成文件。

---

## 3. 听觉识别 (Audd.io / Qwen-ASR)

**核心文件**: `backend/.../AuddApiService.java`

**Failover (故障转移) 机制**：
我们在 `MusicRecognitionController.java` 里写了一个聪明的逻辑：
```java
try {
    // 1. 先试着用 Audd 识别
    return auddService.recognize();
} catch (Exception e) {
    // 2. 如果 Audd 挂了或者没钱了，自动切换到阿里的 Qwen-ASR
    return qwenAsrService.recognize();
}
```
这在企业级开发中叫 **高可用 (High Availability)** 设计。

---

## 🎯 课后作业 (AI 篇)

1.  **调戏 AI**：去 `AIController.java` 里，找到构造 Prompt 的地方，把 "专业的音乐评论家" 改成 "一个说话刻薄的毒舌乐评人"。重启后端，看看生成的分析结果是不是变了？
2.  **改变画风**：去 `StableDiffusionService.java`，在 `prompt` 拼接的地方，强制加上 ", van gogh style" (梵高风格)。看看生成的专辑封面会不会变成油画？
3.  **观察流量**：当你点击生成时，观察终端窗口。你会发现 SD 的终端疯狂滚动进度条，而后端终端在静静等待。这就是 **同步与异步** 的直观体现。
