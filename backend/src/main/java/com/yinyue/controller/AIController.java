package com.yinyue.controller;

import com.yinyue.ai.image.StableDiffusionService;
import com.yinyue.dto.AnalysisResultData;
import com.yinyue.dto.GenerationHistoryItem;
import com.yinyue.service.MusicDesignAnalysisService;
import com.yinyue.service.MusicTrackService;
import com.yinyue.service.MusicImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final StableDiffusionService stableDiffusionService;
    private final MusicTrackService musicTrackService;
    private final MusicImportService musicImportService;
    private final MusicDesignAnalysisService musicDesignAnalysisService;

    @Autowired
    public AIController(StableDiffusionService stableDiffusionService,
                        MusicTrackService musicTrackService,
                        MusicImportService musicImportService,
                        MusicDesignAnalysisService musicDesignAnalysisService) {
        this.stableDiffusionService = stableDiffusionService;
        this.musicTrackService = musicTrackService;
        this.musicImportService = musicImportService;
        this.musicDesignAnalysisService = musicDesignAnalysisService;
    }

    /**
     * 生成 AI 图像
     * 请求参数：
     * - prompt: 提示词 (必填)
     * - negative_prompt: 反向提示词
     * - width: 宽度 (默认 512)
     * - height: 高度 (默认 512)
     * - steps: 步数 (默认 20)
     * - cfg_scale: 相关性 (默认 7.0)
     * - sampler_name: 采样器 (默认 Euler a)
     * - seed: 种子 (默认 -1)
     * - enable_hires: 是否启用高清修复 (默认 false)
     * - hr_scale: 放大倍数 (默认 2.0)
     * - hr_upscaler: 放大算法 (默认 Latent)
     * - denoising_strength: 重绘幅度 (默认 0.7)
     */
    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "Prompt不能为空"));
            }

            StableDiffusionService.ImageGenerationOptions.Builder builder =
                    StableDiffusionService.ImageGenerationOptions.builder()
                            .prompt(prompt);

            if (request.containsKey("negative_prompt")) builder.negativePrompt((String) request.get("negative_prompt"));
            if (request.containsKey("width")) builder.width(((Number) request.get("width")).intValue());
            if (request.containsKey("height")) builder.height(((Number) request.get("height")).intValue());
            if (request.containsKey("steps")) builder.steps(((Number) request.get("steps")).intValue());
            if (request.containsKey("cfg_scale")) builder.cfgScale(((Number) request.get("cfg_scale")).doubleValue());
            if (request.containsKey("sampler_name")) builder.samplerName((String) request.get("sampler_name"));
            if (request.containsKey("seed")) builder.seed(((Number) request.get("seed")).longValue());

            // 高清修复
            if (request.containsKey("enable_hires") && Boolean.TRUE.equals(request.get("enable_hires"))) {
                builder.enableHires(true);
                if (request.containsKey("hr_scale")) builder.hrScale(((Number) request.get("hr_scale")).doubleValue());
                if (request.containsKey("hr_upscaler")) builder.hrUpscaler((String) request.get("hr_upscaler"));
                if (request.containsKey("denoising_strength")) builder.denoisingStrength(((Number) request.get("denoising_strength")).doubleValue());
            }

            String base64Image = stableDiffusionService.generateImage(builder.build());

            // 保存到数据库
            musicTrackService.saveGeneratedCover(prompt, base64Image);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "生成成功");
            response.put("data", base64Image);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "message", "生成失败：" + e.getMessage()));
        }
    }

    /**
     * 获取历史生成记录
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            List<GenerationHistoryItem> history = musicTrackService.getGenerationHistory();
            return ResponseEntity.ok(Map.of("code", 200, "data", history));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "message", "获取历史记录失败：" + e.getMessage()));
        }
    }

    /**
     * AI 分析音乐信息，生成专辑设计建议
     * 请求格式：
     * {
     *   "title": "歌名",
     *   "artist": "歌手",
     *   "album": "专辑",
     *   "lyrics": "歌词",
     *   "audioProfile": {...},
     *   "semanticProfile": {...}
     * }
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMusic(@RequestBody Map<String, Object> request) {
        try {
            AnalysisResultData analysis = musicDesignAnalysisService.analyze(
                    readString(request.get("title"), "未知"),
                    readString(request.get("artist"), "未知"),
                    readString(request.get("album"), "未知"),
                    readString(request.get("lyrics"), ""),
                    request.get("audioProfile"),
                    request.get("semanticProfile"),
                    request.get("referenceHints"),
                    request.get("styleConstraints")
            );

            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分析成功");
            response.put("data", analysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "分析失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 带参考约束生成图像（MVP 先复用 generate-image 逻辑）
     */
    @PostMapping("/generate/with-reference")
    public ResponseEntity<?> generateImageWithReference(@RequestBody Map<String, Object> request) {
        return generateImage(request);
    }

    private String readString(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    /**
     * 网易云链接解析 (POST)
     */
    @PostMapping("/netease/parse")
    public ResponseEntity<?> parseNeteaseUrl(@RequestBody Map<String, String> request) {
        String url = request.getOrDefault("url", "");
        return processNeteaseParsing(url);
    }

    /**
     * 网易云链接解析 (GET) - 适配前端
     */
    @GetMapping("/netease")
    public ResponseEntity<?> parseNeteaseUrlGet(@RequestParam("link") String link) {
        return processNeteaseParsing(link);
    }

    private ResponseEntity<?> processNeteaseParsing(String url) {
        try {
            Map<String, Object> musicInfo = musicImportService.parseNeteaseUrl(url);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "解析成功");
            response.put("data", musicInfo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "message", "解析失败：" + e.getMessage()));
        }
    }
}
