package com.yinyue.ai.asr;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

/**
 * ======================================================================================
 * 【类名】QwenAsrService (语音识别服务 - Automatic Speech Recognition)
 * 
 * 【核心作用】
 * 这个类是项目中的"速记员"。它负责把音频文件（mp3、wav 等）转换成文字。
 * 通过调用阿里云的通义千问 ASR 模型，提供高精度的语音识别能力。
 * 
 * 【使用场景】
 * 1️⃣ 用户上传音乐，但系统识别不出歌名或歌词
 * 2️⃣ 用户录制语音备忘，需要转成文字
 * 3️⃣ 需要从音频中提取文本内容
 * 
 * 【工作流程】
 * 本地音频文件 
 *     ↓
 * 读取 → Base64 编码 
 *     ↓
 * 构建 HTTP 请求包 
 *     ↓
 * 发送给阿里云 Qwen ASR API
 *     ↓
 * 返回识别结果（中文文本）
 * 
 * 【为什么需要这个类】
 * - 隐藏 ASR API 的复杂细节，提供简单易用的接口
 * - 统一管理音频文件的处理逻辑
 * - 集中配置阿里云 API 密钥和端点
 * - 便于添加日志、重试、缓存等功能
 * 
 * 【依赖外部服务】
 * - 阿里云 DashScope API (需要有效的 API Key)
 * - 使用模型：qwen3-asr-flash (快速 ASR 模型)
 * ======================================================================================
 */
@Service
public class QwenAsrService {

    // 【阿里云 API 密钥】
    // 读取来源：application.yml 中的 app.ai.qwen.api-key
    // 用途：向阿里云证明你有权使用 ASR 服务（像入场券一样）
    // 格式通常：sk-xxxx（Secret Key）
    // 获取方式：登录阿里云控制台 → DashScope → API Key
    // 如果未配置或为空：transcribeAudio() 方法会直接返回空字符串（graceful degradation）
    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    // 【使用的 ASR 模型名称】
    // 读取来源：application.yml 中的 app.ai.qwen.model-asr
    // 默认值："qwen3-asr-flash" (速度快的模型)
    // 可选模型：
    //   - qwen3-asr-flash: 快速识别，推荐用于实时应用
    //   - qwen-asr: 标准模型，识别准度较高
    // 选择建议：如果对速度有要求就用 flash，对准度有要求就用标准版
    @Value("${app.ai.qwen.model-asr:qwen3-asr-flash}")
    private String modelAsr;

    // 【阿里云 ASR 服务的 API 端点地址】
    // 读取来源：application.yml 中的 app.ai.qwen.asr-endpoint
    // 默认值：https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcriptions
    // 用途：这是阿里云 ASR 服务的网络地址，所有请求都发往这个地址
    // 注意：这是官方的标准端点地址，一般不需要改动
    @Value("${app.ai.qwen.asr-endpoint:https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcriptions}")
    private String asrEndpoint;

    // 【Spring 提供的 HTTP 请求工具】
    // 用途：用来发送 HTTP POST 请求到阿里云服务
    // 特点：自动处理 HTTP 连接、超时、异常等细节
    // 从 Spring 容器自动注入（不需要手动 new）
    private final RestTemplate restTemplate;
    
    // 【JSON 数据转换工具】
    // 用途：把 Java 对象（如 HashMap）转换成 JSON 字符串，或反之
    // 为什么需要：网络传输只能用文本（JSON），不能传 Java 对象
    // 库：Google Gson，是业界标准的 JSON 处理库
    private final Gson gson;

    public QwenAsrService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.gson = new Gson();
    }

    /**
     * 【方法名】transcribeAudio (把音频转成文字)
     * 
     * 【功能说明】
     * 这是核心方法，负责把本地的音频文件（mp3、wav 等）转换成文字。
     * 通过 HTTP 调用阿里云 Qwen ASR 服务，获得语音识别结果。
     * 
     * 【工作逻辑】
     * 步骤1：检查 API Key 是否有效（没有就直接返回空）
     * 步骤2：检查音频文件是否存在
     * 步骤3：读取音频文件的字节数据
     * 步骤4：转成 Base64 字符串（便于网络传输）
     * 步骤5：构建 HTTP 请求包，包含音频格式、内容、语言等信息
     * 步骤6：发送到阿里云 ASR API
     * 步骤7：解析响应，提取文字结果
     * 步骤8：返回文字给调用者
     * 
     * 【关键细节】
     * - 自动识别文件格式（mp3、wav、m4a 等）并在请求中说明
     * - 固定使用中文识别（"language": "zh"），适应项目需求
     * - 使用 Base64 编码音频数据，确保跨网络传输的兼容性
     * - 响应结果可能很复杂，直接提取 output.text 字段
     * 
     * 【异常处理】
     * - 如果 API Key 为空或无效：直接返回空字符串（优雅降级）
     * - 如果文件不存在：抛出 IllegalArgumentException
     * - 如果 API 调用失败：抛出 RuntimeException，包含错误信息
     * 
     * 【性能考虑】
     * - 整个过程网络请求，耗时通常 1-3 秒（取决于音频长度和网络）
     * - 不适合处理超过 1 小时的音频（API 限制）
     * - 建议添加超时控制，避免长期等待
     * 
     * @param audioFile 本地的音频文件对象，必须存在且可读
     * @return 识别出来的文字（中文）
     * @throws IllegalArgumentException 如果文件不存在
     * @throws RuntimeException 如果 API 调用失败
     */
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1500, multiplier = 2.0),
        recover = "recoverFromAsrError"
    )
    public String transcribeAudio(File audioFile) {
        // 【前置条件检查】
        // 检查 API Key 是否配置。有几种"坏"的情况：
        // 1. apiKey = null（没配置）
        // 2. apiKey = ""（空字符串）
        // 3. apiKey.startsWith("your_")（是示例占位符，比如 "your_api_key_here"）
        // 任何一种情况都意味着没有真正的 API Key，无法调用服务
        // 此时我们优雅地返回空字符串，而不是报错
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("your_")) {
            return "";
        }
        
        try {
            // 【步骤1：验证输入文件】
            // 文件可能在调用方删除了，或者路径输入错误
            // 提前检查可以避免后续复杂的处理流程
            if (audioFile == null || !audioFile.exists()) {
                throw new IllegalArgumentException("音频文件不存在");
            }

            // 【步骤2-3：读取文件并编码】
            // Files.readAllBytes() 一次性读取整个文件的所有字节
            // 注意：大文件（>500MB）可能导致内存溢出，实际应用中需要分块处理
            byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
            // Base64 编码的原因：网络传输只支持文本，二进制数据需要转换成文本格式
            // 编码后大小约为原始大小的 133%（4个字符编码3字节）
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // 【步骤4：构建音频对象】
            // 阿里云 API 要求音频数据必须包含：
            // - format: 文件格式（mp3、wav、flac 等），用于正确解析
            // - content: Base64 编码的音频数据
            Map<String, Object> audioObject = new HashMap<>();
            audioObject.put("format", getFileExtension(audioFile.getName()));
            audioObject.put("content", audioBase64);

            // 【步骤5：构建输入对象】
            // API 支持一次上传多个音频文件，所以 "audio" 是一个列表
            // 虽然我们现在只传一个，但这样保持与 API 规范一致
            Map<String, Object> input = new HashMap<>();
            input.put("audio", Collections.singletonList(audioObject));

            // 【步骤6：设置识别参数】
            // language: "zh" 告诉 ASR 模型语言是中文
            // 这很重要，因为不同语言的模型识别效果差异很大
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("language", "zh");

            // 【步骤7：最终的请求包】
            // 这就是要发送给阿里云的完整请求数据
            // 结构：{ model: "...", input: { audio: [...] }, parameters: { ... } }
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelAsr);
            payload.put("input", input);
            payload.put("parameters", parameters);

            // 【步骤8：准备 HTTP 头】
            // HTTP 请求需要头信息来告诉服务器请求的格式和身份
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);  // 告诉服务器：请求体是 JSON
            headers.set("Authorization", "Bearer " + apiKey);   // 亮出 API Key，证明身份
            // "Bearer" 是 OAuth 2.0 标准的授权格式

            // 【步骤9：组装完整请求】
            // HttpEntity 是 Spring 的请求包装类，包含请求体和请求头
            // gson.toJson(payload) 把 Java HashMap 转成 JSON 字符串
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(payload), headers);

            // 【步骤10：发送请求到阿里云】
            // restTemplate.postForObject() 方法：
            //   - 第1个参数：目标 URL
            //   - 第2个参数：完整的请求（包含头和体）
            //   - 第3个参数：期望的返回类型（这里是 Map.class）
            // 这是一个同步调用，会一直等到收到响应或超时
            Map response = restTemplate.postForObject(
                    asrEndpoint,
                    request,
                    Map.class
            );

            // 【步骤11-14：解析响应】
            // 首先检查响应是否为空（异常情况）
            if (response == null) {
                throw new RuntimeException("ASR 响应为空");
            }

            // 阿里云返回的响应结构通常如下：
            // {
            //   "status": 200 或错误码,
            //   "output": {
            //     "text": "识别出的文字",
            //     "duration": 12345 (音频时长，毫秒),
            //     ...
            //   },
            //   "usage": { ... }
            // }
            // 我们需要从这个嵌套结构中提取出 output.text
            Object outputObj = response.get("output");
            if (outputObj instanceof Map) {
                Map output = (Map) outputObj;
                Object textObj = output.get("text");
                if (textObj != null) {
                    // 成功！返回识别出的文字
                    return textObj.toString();
                }
            }

            // 如果没有找到标准的 text 字段，把整个响应转成 JSON 字符串返回
            // 这有利于调试，能看到实际返回的格式
            return gson.toJson(response);
            
        } catch (Exception e) {
            // 任何异常都包装成 RuntimeException，包含详细的错误信息
            // e 是原始异常，getStackTrace() 信息保留了，便于定位问题
            throw new RuntimeException("阿里语音识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 【方法名】getFileExtension (提取文件后缀)
     * 
     * 【功能说明】
     * 从文件名中提取文件后缀（扩展名），比如 "song.mp3" → "mp3"。
     * 这个后缀告诉 ASR 模型文件的音频格式，使其能正确解析。
     * 
     * 【工作逻辑】
     * 1. 找文件名中最后一个点（.）的位置
     * 2. 如果找到了，提取点之后的部分
     * 3. 转成小写字母（因为有的人写 .MP3，有的写 .mp3，需要统一）
     * 4. 如果找不到点或文件名为空，默认返回 "wav"（容错处理）
     * 
     * 【代码细节】
     * - lastIndexOf('.')：在字符串中找最后一个点的位置
     * - idx > 0：确保点不在最前面（比如 ".hidden" 文件）
     * - idx < name.length() - 1：确保点不在最后（比如 "file."）
     * - substring(idx + 1)：从点后面开始截取到末尾
     * - toLowerCase()：转成小写统一格式
     * 
     * 【支持的格式】
     * - mp3（常见）
     * - wav（常见）
     * - m4a（苹果设备）
     * - flac（高保真）
     * - aac（高级音频编码）
     * - ogg（开源格式）
     * 只要是标准的音频格式，ASR 模型都能处理
     * 
     * 【示例】
     * getFileExtension("song.mp3") → "mp3"
     * getFileExtension("voice.WAV") → "wav"（自动小写）
     * getFileExtension("recording.m4a") → "m4a"
     * getFileExtension("noextension") → "wav"（默认值）
     * getFileExtension(null) → "wav"（空值处理）
     * 
     * @param name 文件名（如 "song.mp3"）
     * @return 文件后缀，小写字母（如 "mp3"）
     */
    private String getFileExtension(String name) {
        // 【防守性编程】
        // 首先检查 name 是否为 null
        // 如果为 null，直接跳到最后返回默认值 "wav"
        if (name != null) {
            // 【查找最后一个点】
            // lastIndexOf 返回最后一个点的位置索引
            // 比如 "song.mp3" 中，点在索引 4 的位置，所以返回 4
            // 如果没有点，返回 -1
            int idx = name.lastIndexOf('.');
            
            // 【有效性检查】
            // idx > 0：点不在第一个位置（排除 ".hidden" 这样的隐藏文件）
            // idx < name.length() - 1：点不在最后一个位置（排除 "filename." 这样的畸形文件名）
            // 两个条件都满足，说明点在中间，后面肯定还有字符
            if (idx > 0 && idx < name.length() - 1) {
                // 【提取后缀】
                // substring(idx + 1) 从点后面的下一个字符开始截取到末尾
                // 比如 "song.mp3", idx=4, substring(5) = "mp3"
                // toLowerCase() 把大写字母转成小写
                // 比如 "MP3" → "mp3"，保证格式一致
                return name.substring(idx + 1).toLowerCase();
            }
        }
        
        // 【容错处理】
        // 如果上面的逻辑没有返回（比如 name 为 null、没有点、或点位置不对）
        // 就默认返回 "wav"
        // wav 是音频的通用格式，大多数 ASR 模型都能识别
        // 这样保证了方法一定能返回一个有效的格式字符串
        return "wav";
    }

    /**
     * 重试失败时的恢复方法，返回空字符串
     * 这样尽管大事不好，但永远不会报错
     */
    private String recoverFromAsrError(Exception e) {
        System.err.println("Qwen ASR 重试失败，返回空字符串: " + e.getMessage());
        return "";
    }
}
