package com.yinyue.controller;

import com.yinyue.ai.asr.QwenAsrService;
import com.yinyue.ai.music.AuddApiService;
import com.yinyue.ai.llm.QwenLLMService;
import com.yinyue.ai.image.StableDiffusionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/music")
@CrossOrigin(origins = "*")
public class MusicRecognitionController {

    @Autowired
    private AuddApiService auddApiService;

    @Autowired
    private QwenLLMService qwenLLMService;

    @Autowired
    private StableDiffusionService stableDiffusionService;

    @Autowired
    private QwenAsrService qwenAsrService;

    /**
     * 完整流程：上传音频 -> 识别 -> AI 分析 -> 生成封面
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMusicFile(@RequestParam("filePath") String filePath) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 1. 识别音乐
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                throw new RuntimeException("音频文件不存在: " + filePath);
            }

            String recognitionResult = auddApiService.recognizeAudio(audioFile);
            AuddApiService.MusicInfo musicInfo = auddApiService.parseResult(recognitionResult);

            result.put("musicInfo", musicInfo);

            // 2. AI 分析歌词和风格（使用 Qwen）
            String analysisPrompt = String.format(
                    "请分析这首音乐，并根据歌名、歌手、专辑生成专辑封面设计建议。\n" +
                    "歌名: %s\n歌手: %s\n专辑: %s\n",
                    musicInfo != null ? musicInfo.getTitle() : "未知",
                    musicInfo != null ? musicInfo.getArtist() : "未知",
                    musicInfo != null ? musicInfo.getAlbum() : "未知"
            );

            String aiAnalysis = qwenLLMService.callQwenLLM(analysisPrompt);
            result.put("aiAnalysis", aiAnalysis);

            // 3. 生成专辑封面
            String albumTitle = musicInfo != null ? musicInfo.getTitle() : "音乐专辑";
            String coverPrompt = String.format("为《%s》生成专业的专辑封面，基于以下分析: %s",
                    albumTitle, aiAnalysis);

            String imageBase64 = stableDiffusionService.generateAlbumCover(coverPrompt);
            result.put("albumCover", imageBase64);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "音乐分析完成");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "分析失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 仅识别音乐
     */
    @PostMapping("/recognize")
    public ResponseEntity<?> recognizeMusic(@RequestParam("filePath") String filePath) {
        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                throw new RuntimeException("音频文件不存在: " + filePath);
            }

            AuddApiService.MusicInfo musicInfo = null;
            try {
                String result = auddApiService.recognizeAudio(audioFile);
                musicInfo = auddApiService.parseResult(result);
            } catch (Exception e) {
                System.err.println("Audd 识别失败，尝试使用阿里语音识别: " + e.getMessage());
            }

            if (musicInfo == null || isEmpty(musicInfo.getTitle()) && isEmpty(musicInfo.getArtist()) && isEmpty(musicInfo.getAlbum())) {
                String text = qwenAsrService.transcribeAudio(audioFile);
                if (text != null && !text.isEmpty()) {
                    if (musicInfo == null) {
                        musicInfo = new AuddApiService.MusicInfo();
                    }
                    musicInfo.setTitle("未知");
                    musicInfo.setArtist("未知");
                    musicInfo.setAlbum("未知");
                    musicInfo.setLyrics(text);
                    musicInfo.setRawJson(text);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "识别成功");
            response.put("data", musicInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "识别失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 识别 URL 音乐
     */
    @PostMapping("/recognize-url")
    public ResponseEntity<?> recognizeMusicUrl(@RequestParam("url") String audioUrl) {
        try {
            String result = auddApiService.recognizeAudioUrl(audioUrl);
            AuddApiService.MusicInfo musicInfo = auddApiService.parseResult(result);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "识别成功");
            response.put("data", musicInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "识别失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
