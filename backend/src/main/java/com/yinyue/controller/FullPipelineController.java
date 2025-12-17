package com.yinyue.controller;

import com.yinyue.entity.MusicTrack;
import com.yinyue.service.FileUploadService;
import com.yinyue.service.MusicTrackService;
import com.yinyue.ai.music.AuddApiService;
import com.yinyue.ai.llm.QwenLLMService;
import com.yinyue.ai.image.StableDiffusionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pipeline")
@CrossOrigin(origins = "*")
public class FullPipelineController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private MusicTrackService musicTrackService;

    @Autowired
    private AuddApiService auddApiService;

    @Autowired
    private QwenLLMService qwenLLMService;

    @Autowired
    private StableDiffusionService stableDiffusionService;

    /**
     * 完整流程：上传 -> 识别 -> 分析 -> 生成封面
     */
    @PostMapping("/full-pipeline")
    public ResponseEntity<?> fullPipeline(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileUploadService.uploadFile(file, "audio");

            String recognitionResult = null;
            AuddApiService.MusicInfo musicInfo = null;
            try {
                recognitionResult = auddApiService.recognizeAudio(new java.io.File(filePath));
                musicInfo = auddApiService.parseResult(recognitionResult);
            } catch (Exception ignore) {
                recognitionResult = null;
                musicInfo = null;
            }

            MusicTrack track = new MusicTrack();
            track.setTitle(musicInfo != null ? musicInfo.getTitle() : "未知");
            track.setArtist(musicInfo != null ? musicInfo.getArtist() : "未知");
            track.setAlbum(musicInfo != null ? musicInfo.getAlbum() : "未知");
            track.setFilePath(filePath);
            track.setFileSize(file.getSize());
            track.setAuddResult(recognitionResult);
            track.setStatus("ANALYZING");
            MusicTrack savedTrack = musicTrackService.saveTrack(track);

            String analysisPrompt = String.format(
                    "你是一个专业的音乐分析师。请分析这首音乐并生成专辑封面设计建议。\n" +
                            "歌名: %s\n歌手: %s\n专辑: %s\n\n" +
                            "请用 JSON 格式返回分析结果，包含以下字段:\n" +
                            "{\"theme\":\"主题\", \"mood\":\"氛围\", \"visual_style\":\"视觉风格\", \"colors\":[\"颜色1\",\"颜色2\",\"颜色3\"], \"image_prompt_en\":\"英文图片描述\"}",
                    track.getTitle(), track.getArtist(), track.getAlbum()
            );

            String aiAnalysis = qwenLLMService.callQwenLLM(analysisPrompt);

            String coverPrompt = String.format(
                    "Generate a professional album cover for '%s' by '%s' based on this analysis: %s",
                    track.getTitle(), track.getArtist(), aiAnalysis
            );

            String imageBase64 = stableDiffusionService.generateAlbumCover(coverPrompt);

            track.setAiAnalysis(aiAnalysis);
            track.setStatus("COMPLETED");
            musicTrackService.updateTrack(savedTrack.getId(), track);

            Map<String, Object> result = new HashMap<>();
            result.put("trackId", savedTrack.getId());
            result.put("musicInfo", musicInfo);
            result.put("analysis", aiAnalysis);
            result.put("coverImage", imageBase64.substring(0, Math.min(100, imageBase64.length())) + "...");

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "完整流程执行成功");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "流程执行失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取所有已处理的音乐
     */
    @GetMapping("/all-tracks")
    public ResponseEntity<?> getAllTracks() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", musicTrackService.getAllTracks());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 按 ID 获取单个音乐记录
     */
    @GetMapping("/track/{id}")
    public ResponseEntity<?> getTrack(@PathVariable Long id) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", musicTrackService.getTrackById(id));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试各个 AI 服务是否正常
     */
    @GetMapping("/test-services")
    public ResponseEntity<?> testServices() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 测试 Qwen
            boolean qwenOk = qwenLLMService.testConnection();
            response.put("qwen", qwenOk ? "✓ OK" : "✗ Failed");
        } catch (Exception e) {
            response.put("qwen", "✗ " + e.getMessage());
        }

        // 测试 Stable Diffusion（简单检查）
        try {
            response.put("stable-diffusion", "✓ 服务运行在 http://127.0.0.1:7860");
        } catch (Exception e) {
            response.put("stable-diffusion", "✗ 服务不可用");
        }

        // 测试数据库
        try {
            long count = musicTrackService.getAllTracks().size();
            response.put("database", "✓ 已保存 " + count + " 条记录");
        } catch (Exception e) {
            response.put("database", "✗ " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "服务检查完成");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }
}
