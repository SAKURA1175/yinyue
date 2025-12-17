package com.yinyue.ai.music;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
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
import com.google.gson.JsonObject;

/**
 * ======================================================================================
 * 类名：AuddApiService (听歌识曲服务)
 * 
 * 作用：这个类就像是一个“耳朵”。
 * 当用户上传一首歌，但是没有填歌名、歌手的时候，我们就用这个类去“听”一下这首歌。
 * 它会把声音发给 Audd.io 这个网站，然后告诉我们这首歌叫什么名字。
 * ======================================================================================
 */
@Service
public class AuddApiService {

    // 【Audd API 密钥】
    // 读取来源：application.yml 中的 app.ai.audd.api-key
    // 用途：向 Audd.io 证明你有权使用识曲服务
    // 负责收、收费針收區控制
    // 获取方式：按访 Audd.io 官网登录账符
    @Value("${app.ai.audd.api-key}")
    private String apiKey;

    // 【Audd 服务的网络地址】
    // 读取来源：application.yml 中的 app.ai.audd.endpoint
    // 默认值：https://api.audd.io/
    // 用途：这是 Audd 服务的网络地址，所有请求都发往这个地址
    // 注意：Audd 是第三方服务，不是阿里云，地址是固定的
    @Value("${app.ai.audd.endpoint:https://api.audd.io/}")
    private String endpoint;

    // 【Spring 提供的 HTTP 请求工具】
    // 用途：用来发送 HTTP POST 请求到 Audd.io 服务
    private final RestTemplate restTemplate;
    
    // 【JSON 数据转换工具】
    // 用途：把 Java 对象转换成 JSON，或把 JSON 解析成 Java 对象
    private final Gson gson;

    public AuddApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.gson = new Gson();
    }

    /**
     * 方法名：recognizeAudio (识别本地文件 - 带重试)
     * 
     * 作用：处理存放在我们硬盘上的音频文件。
     * 如果网络抖动失败，会自动重试最多 3 次，每次间隔 1s、2s、4s
     * 
     * @param audioFile 本地的音频文件对象
     * @return 识别结果（JSON 字符串）
     */
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        recover = "recoverFromAuddError"
    )
    public String recognizeAudio(File audioFile) {
        try {
            // 1. 读取文件：把硬盘上的 mp3 文件变成电脑能看懂的 010101... (字节数组)
            byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
            
            // 2. 编码：把这些 010101... 变成一长串字符 (Base64)。
            // 为什么要这样做？因为直接在网上传送二进制文件比较麻烦，变成字符串就方便多了。
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // 3. 填表：准备要发给 Audd 的数据
            Map<String, Object> payload = new HashMap<>();
            payload.put("api_token", apiKey); // 贴上通行证
            payload.put("audio", audioBase64); // 放上录音数据

            // 4. 准备信封
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(payload), headers);

            // 5. 发送请求
            // 这里的 "recognize" 是 Audd API 规定的接口名字
            Map response = restTemplate.postForObject(
                    endpoint + "recognize",
                    request,
                    Map.class
            );

            // 6. 把结果转回字符串给我们看
            return gson.toJson(response);

        } catch (IOException e) {
            // 如果读取文件的时候硬盘坏了或者文件不见了
            throw new RuntimeException("音频识别失败: " + e.getMessage());
        }
    }

    /**
     * 方法名：recognizeAudioUrl (识别网络链接 - 带重试)
     * 
     * 作用：如果音频文件已经在网上了（比如有了一个 http://... 的链接），
     * 我们就不用自己下载再上传了，直接把链接给 Audd，让它自己去下载识别。
     * 这样速度更快，也省流量。
     * 
     * @param audioUrl 音频文件的网址
     * @return 识别结果
     */
    @Retryable(
        maxAttempts = 2,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public String recognizeAudioUrl(String audioUrl) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("api_token", apiKey); // 通行证
            payload.put("url", audioUrl);     // 直接给网址

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(payload), headers);

            Map response = restTemplate.postForObject(
                    endpoint + "recognize",
                    request,
                    Map.class
            );

            return gson.toJson(response);

        } catch (Exception e) {
            throw new RuntimeException("音频 URL 识别失败: " + e.getMessage());
        }
    }

    /**
     * 方法名：parseResult (整理结果)
     * 
     * 作用：Audd 返回的数据可能很乱，而且有很多我们不需要的信息。
     * 这个方法就像是一个“分拣员”，把我们真正关心的（歌名、歌手、专辑）挑出来，
     * 放进一个整整齐齐的盒子（MusicInfo 对象）里。
     * 
     * @param resultJson 原始的乱糟糟的 JSON
     * @return 整理好的 MusicInfo 对象
     */
    public MusicInfo parseResult(String resultJson) {
        try {
            // 把字符串变成 JSON 对象，方便查找
            JsonObject jsonObject = gson.fromJson(resultJson, JsonObject.class);
            
            // 检查有没有 "result" 这个字段
            if (jsonObject.has("result")) {
                JsonObject result = jsonObject.getAsJsonObject("result");
                
                // 创建一个新的盒子
                MusicInfo info = new MusicInfo();
                
                // 假如结果里有 title，就拿出来；如果没有，就填个空字符串
                info.setTitle(result.has("title") ? result.get("title").getAsString() : "");
                info.setArtist(result.has("artist") ? result.get("artist").getAsString() : "");
                info.setAlbum(result.has("album") ? result.get("album").getAsString() : "");
                
                // 把原始数据也存一份，以防万一
                info.setRawJson(resultJson);
                
                return info;
            }
        } catch (Exception e) {
            // 如果解析过程出错了（比如 JSON 格式不对），就装作无事发生，返回 null。
        }
        
        return null;
    }

    /**
     * 类名：MusicInfo (音乐信息盒)
     * 
     * 作用：这是一个 DTO (Data Transfer Object)，专门用来在程序各处搬运数据的。
     * 它没有任何复杂的功能，只有一堆属性（歌名、歌手...）和存取方法（Get/Set）。
     */
    public static class MusicInfo {
        private String title;   // 歌名
        private String artist;  // 歌手
        private String album;   // 专辑
        private String rawJson; // 原始数据
        private String lyrics;  // 歌词

        // =================================================================
        // 下面这些是 Getter (取值) 和 Setter (赋值) 方法
        // =================================================================
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
        
        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }
        
        public String getRawJson() { return rawJson; }
        public void setRawJson(String rawJson) { this.rawJson = rawJson; }
        
        public String getLyrics() { return lyrics; }
        public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    }

    /**
     * 重试失败时的恢复方法，返回空 JSON
     * 这样尽管大事不好，但永远不会报错
     */
    private String recoverFromAuddError(Exception e) {
        System.err.println("Audd API 重试失败，返回空 JSON: " + e.getMessage());
        return "{\"status\":\"error\",\"message\":\"Music recognition service unavailable\"}";
    }
}
