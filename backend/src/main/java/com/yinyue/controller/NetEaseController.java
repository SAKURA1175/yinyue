package com.yinyue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/netease")
@CrossOrigin(origins = "*")
public class NetEaseController {

    /**
     * 从网易云链接中解析歌曲ID并获取歌曲信息
     * 支持的链接格式：
     * - https://music.163.com/song?id=123456789
     * - https://music.163.com/album?id=123456789
     * - https://music.163.com/playlist?id=123456789
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseNeteaseUrl(@RequestBody Map<String, String> request) {
        try {
            String url = request.getOrDefault("url", "");

            if (url.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "code", 400,
                        "message", "URL不能为空"
                ));
            }

            // 提取ID
            String id = extractIdFromUrl(url);
            if (id == null || id.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "code", 400,
                        "message", "无效的网易云链接格式"
                ));
            }

            // 确定链接类型
            String type = extractTypeFromUrl(url);

            // 获取歌曲信息（这里使用模拟数据，实际应调用网易云API）
            Map<String, Object> musicInfo = fetchNeteaseInfo(id, type);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "解析成功");
            response.put("data", musicInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "解析失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 从URL中提取歌曲/专辑/播放列表ID
     */
    private String extractIdFromUrl(String url) {
        // 匹配 id=xxx 的格式
        Pattern pattern = Pattern.compile("[?&]id=([0-9]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 确定URL类型：song/album/playlist
     */
    private String extractTypeFromUrl(String url) {
        if (url.contains("/song?")) {
            return "song";
        } else if (url.contains("/album?")) {
            return "album";
        } else if (url.contains("/playlist?")) {
            return "playlist";
        }
        return "song"; // 默认类型
    }

    /**
     * 获取网易云音乐信息（模拟实现）
     * 实际项目应调用网易云API或三方接口
     */
    private Map<String, Object> fetchNeteaseInfo(String id, String type) {
        Map<String, Object> info = new HashMap<>();

        // 这里是模拟数据，实际应通过API获取
        // 可以调用: https://music.163.com/api/song/detail?ids={id}
        // 或使用第三方库: NeteaseCloudMusicApi

        info.put("id", id);
        info.put("title", "网易云音乐");
        info.put("artist", "未知歌手");
        info.put("album", "未知专辑");
        info.put("type", type);
        info.put("cover_url", "");
        info.put("lyrics", "");

        // 如果需要真实数据，可以在这里调用 RestTemplate 或 WebClient
        // String apiUrl = "https://music.163.com/api/song/detail?ids=" + id;
        // ... 调用API获取真实数据 ...

        return info;
    }
}
