package com.yinyue.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yinyue.ai.image.StableDiffusionService;
import com.yinyue.ai.llm.QwenLLMService;
import com.yinyue.service.MusicTrackService;
import com.yinyue.entity.MusicTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private QwenLLMService qwenLLMService;

    @Autowired
    private StableDiffusionService stableDiffusionService;

    @Autowired
    private MusicTrackService musicTrackService;

    private final Gson gson = new Gson();

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
            List<MusicTrack> history = musicTrackService.getAllHistory();
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
     *   "lyrics": "歌词"
     * }
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMusic(@RequestBody Map<String, String> request) {
        try {
            String title = request.getOrDefault("title", "未知");
            String artist = request.getOrDefault("artist", "未知");
            String album = request.getOrDefault("album", "未知");
            String lyrics = request.getOrDefault("lyrics", "");

            // 构建 AI 分析 Prompt
            String prompt = String.format(
                    "你是一个专业的音乐分析师和视觉设计顾问。你的任务是为音乐设计一张极具艺术感的专辑封面。\n\n" +
                    "【音乐信息】\n" +
                    "歌名：%s\n" +
                    "歌手：%s\n" +
                    "专辑：%s\n" +
                    "%s\n\n" +
                    "【重要指令】\n" +
                    "1. 如果上述信息（歌名/歌手/歌词）包含'未知'或为空，请不要直接返回'未知'。你需要发挥想象力，随机构思一个独特的音乐意境（例如：'赛博朋克霓虹夜'、'静谧的森林晨曦'、'失落的深海文明'、'复古蒸汽波'等），并基于这个构思填充分析结果。\n" +
                    "2. 每一个字段都必须填入具体、生动的描述性文字，禁止使用'N/A'、'None'或'未知'。\n" +
                    "3. 'image_prompt_en' 必须是高质量的 Stable Diffusion 英文提示词，包含光影、材质、风格等细节词。\n\n" +
                    "请严格按照以下 JSON 格式返回（不要包含 Markdown 代码块或 ```json 标记，直接返回 JSON 字符串）：\n" +
                    "{\n" +
                    "  \"theme\": \"核心主题（例如：时空错位的孤独感）\",\n" +
                    "  \"mood\": \"情感氛围（例如：忧郁但充满希望）\",\n" +
                    "  \"visual_style\": \"视觉风格（例如：Low-poly 极简主义，冷色调）\",\n" +
                    "  \"colors\": [\"#HexColor1\", \"#HexColor2\", \"#HexColor3\"],\n" +
                    "  \"image_prompt_en\": \"(英文) masterpiece, best quality, album cover, ...\"\n" +
                    "}",
                    title, artist, album,
                    lyrics.isEmpty() ? "" : "歌词片段：" + (lyrics.length() > 500 ? lyrics.substring(0, 500) + "..." : lyrics)
            );

            // 调用 Qwen LLM
            String analysisResult = qwenLLMService.callQwenLLM(prompt);

            // 解析 JSON 结果
            Map<String, Object> analysis = parseAnalysisResult(analysisResult);

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
     * 解析 AI 返回的 JSON 结果
     */
    private Map<String, Object> parseAnalysisResult(String jsonString) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 使用 Gson 解析 JSON
            Map<String, Object> parsed = gson.fromJson(jsonString, new TypeToken<Map<String, Object>>(){}.getType());
            
            result.put("theme", parsed.getOrDefault("theme", "未知"));
            result.put("mood", parsed.getOrDefault("mood", "未知"));
            result.put("visual_style", parsed.getOrDefault("visual_style", "未知"));
            
            Object colorsObj = parsed.get("colors");
            if (colorsObj instanceof List) {
                result.put("colors", ((List<?>) colorsObj).toArray(new String[0]));
            } else {
                result.put("colors", new String[]{"#D93C39", "#FF6565", "#FFB3B0"});
            }

            String prompt = (String) parsed.getOrDefault("image_prompt_en", "");
            if (prompt.isEmpty()) {
                prompt = (String) parsed.getOrDefault("prompt", "");
            }
            result.put("image_prompt_en", prompt.isEmpty() ? "Generate a professional album cover" : prompt);

            return result;

        } catch (Exception e) {
            // 解析失败时尝试手动提取或返回默认值
            System.err.println("Gson parsing failed, falling back to defaults: " + e.getMessage());
            result.put("theme", "未知");
            result.put("mood", "未知");
            result.put("visual_style", "未知");
            result.put("colors", new String[]{"#D93C39", "#FF6565", "#FFB3B0"});
            result.put("image_prompt_en", "Generate a professional album cover");
            return result;
        }
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
            if (url == null || url.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("code", 400, "message", "URL不能为空"));
            }

            String id = extractIdFromUrl(url);
            if (id == null || id.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("code", 400, "message", "无效的网易云链接格式"));
            }

            String type = extractTypeFromUrl(url);
            Map<String, Object> musicInfo = fetchNeteaseInfo(id, type);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "解析成功");
            response.put("data", musicInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "message", "解析失败：" + e.getMessage()));
        }
    }

    private String extractIdFromUrl(String url) {
        Pattern pattern = Pattern.compile("[?&]id=([0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractTypeFromUrl(String url) {
        if (url.contains("/song?")) return "song";
        else if (url.contains("/album?")) return "album";
        else if (url.contains("/playlist?")) return "playlist";
        return "song";
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
            // 调用 Python 脚本解析网易云音乐信息
            String scriptPath = "d:\\yinyue\\netease_parser.py";
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath, id);
            
            // 设置环境变量，强制 Python 输出 UTF-8
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8")
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String jsonResult = output.toString();
                // 使用 Gson 解析
                try {
                    Map<String, Object> parsed = gson.fromJson(jsonResult, new TypeToken<Map<String, Object>>(){}.getType());
                    info.put("title", parsed.getOrDefault("title", "未知"));
                    info.put("artist", parsed.getOrDefault("artist", "未知"));
                    info.put("album", parsed.getOrDefault("album", "未知"));
                    info.put("cover_url", parsed.getOrDefault("cover_url", ""));
                    String lyrics = (String) parsed.getOrDefault("lyrics", "");
                    if (!lyrics.isEmpty()) {
                        info.put("lyrics", lyrics);
                    }
                } catch (Exception e) {
                    System.err.println("Gson parsing failed for Netease result: " + e.getMessage());
                }
            } else {
                System.err.println("Python script failed with exit code: " + exitCode);
                System.err.println("Output: " + output.toString());
            }
            
        } catch (Exception e) {
            System.err.println("Error calling python script: " + e.getMessage());
            e.printStackTrace();
        }
        
        return info;
    }


}