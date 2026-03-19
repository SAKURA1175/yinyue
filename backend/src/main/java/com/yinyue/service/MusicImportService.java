package com.yinyue.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MusicImportService {

    private static final Logger log = LoggerFactory.getLogger(MusicImportService.class);
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private final Gson gson = new Gson();

    @Value("${app.import.python-command:python}")
    private String pythonCommand;

    @Value("${app.import.netease-script-path:./netease_parser.py}")
    private String neteaseScriptPath;

    @Value("${app.import.netease-timeout-seconds:15}")
    private long timeoutSeconds;

    public Map<String, Object> parseNeteaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL不能为空");
        }

        String id = extractIdFromUrl(url);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("无效的网易云链接格式");
        }

        return fetchNeteaseInfo(id, extractTypeFromUrl(url));
    }

    private Map<String, Object> fetchNeteaseInfo(String id, String type) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", id);
        info.put("title", "网易云音乐");
        info.put("artist", "未知歌手");
        info.put("album", "未知专辑");
        info.put("cover_url", "");
        info.put("type", type);
        info.put("lyrics", "");

        try {
            Path scriptPath = resolveScriptPath();
            if (!Files.exists(scriptPath)) {
                throw new IllegalStateException("网易云解析脚本不存在: " + scriptPath);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand,
                    scriptPath.toString(),
                    id
            );
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean completed = process.waitFor(Duration.ofSeconds(timeoutSeconds).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("网易云解析超时");
            }

            if (process.exitValue() == 0) {
                Map<String, Object> parsed = gson.fromJson(output.toString(), MAP_TYPE);
                if (parsed != null) {
                    info.put("title", parsed.getOrDefault("title", "未知"));
                    info.put("artist", parsed.getOrDefault("artist", "未知"));
                    info.put("album", parsed.getOrDefault("album", "未知"));
                    info.put("cover_url", parsed.getOrDefault("cover_url", ""));
                    String lyrics = (String) parsed.getOrDefault("lyrics", "");
                    if (!lyrics.isBlank()) {
                        info.put("lyrics", lyrics);
                    }
                }
            } else {
                log.warn("网易云脚本返回非 0 状态码，输出: {}", output);
            }
        } catch (Exception e) {
            log.warn("调用网易云解析脚本失败: {}", e.getMessage());
        }

        return info;
    }

    private Path resolveScriptPath() {
        Path configuredPath = Paths.get(neteaseScriptPath);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                workingDirectory.resolve(configuredPath).normalize(),
                workingDirectory.resolve("..").resolve(configuredPath).normalize()
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        return candidates.get(0);
    }

    private String extractIdFromUrl(String url) {
        Matcher matcher = Pattern.compile("[?&]id=([0-9]+)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractTypeFromUrl(String url) {
        if (url.contains("/song?")) return "song";
        if (url.contains("/album?")) return "album";
        if (url.contains("/playlist?")) return "playlist";
        return "song";
    }
}
