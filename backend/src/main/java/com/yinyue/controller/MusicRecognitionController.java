package com.yinyue.controller;

import com.yinyue.ai.asr.QwenAsrService;
import com.yinyue.ai.music.AuddApiService;
import com.yinyue.ai.image.StableDiffusionService;
import com.yinyue.ai.llm.QwenLLMService;
import com.yinyue.entity.MusicTrack;
import com.yinyue.service.AudioSemanticAnalysisService;
import com.yinyue.service.AudioFeatureAnalysisService;
import com.yinyue.service.FileUploadService;
import com.yinyue.service.MusicDesignAnalysisService;
import com.yinyue.service.MusicTrackService;
import com.yinyue.service.SourceSeparationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/music")
public class MusicRecognitionController {

    private final AuddApiService auddApiService;
    private final QwenLLMService qwenLLMService;
    private final StableDiffusionService stableDiffusionService;
    private final QwenAsrService qwenAsrService;
    private final MusicTrackService musicTrackService;
    private final FileUploadService fileUploadService;
    private final AudioFeatureAnalysisService audioFeatureAnalysisService;
    private final AudioSemanticAnalysisService audioSemanticAnalysisService;
    private final SourceSeparationService sourceSeparationService;
    private final MusicDesignAnalysisService musicDesignAnalysisService;

    public MusicRecognitionController(AuddApiService auddApiService,
                                      QwenLLMService qwenLLMService,
                                      StableDiffusionService stableDiffusionService,
                                      QwenAsrService qwenAsrService,
                                      MusicTrackService musicTrackService,
                                      FileUploadService fileUploadService,
                                      AudioFeatureAnalysisService audioFeatureAnalysisService,
                                      AudioSemanticAnalysisService audioSemanticAnalysisService,
                                      SourceSeparationService sourceSeparationService,
                                      MusicDesignAnalysisService musicDesignAnalysisService) {
        this.auddApiService = auddApiService;
        this.qwenLLMService = qwenLLMService;
        this.stableDiffusionService = stableDiffusionService;
        this.qwenAsrService = qwenAsrService;
        this.musicTrackService = musicTrackService;
        this.fileUploadService = fileUploadService;
        this.audioFeatureAnalysisService = audioFeatureAnalysisService;
        this.audioSemanticAnalysisService = audioSemanticAnalysisService;
        this.sourceSeparationService = sourceSeparationService;
        this.musicDesignAnalysisService = musicDesignAnalysisService;
    }

    /**
     * 完整流程：上传音频 -> 识别 -> AI 分析 -> 生成封面
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMusicFile(@RequestParam(value = "uploadId", required = false) Long uploadId,
                                              @RequestParam(value = "filePath", required = false) String filePath) {
        try {
            Map<String, Object> result = new HashMap<>();
            MusicTrack track = resolveManagedTrack(uploadId, filePath);

            // 1. 识别音乐
            Path audioFile = fileUploadService.resolveManagedFile(track.getFilePath());

            String recognitionResult = auddApiService.recognizeAudio(audioFile.toFile());
            AuddApiService.MusicInfo musicInfo = auddApiService.parseResult(recognitionResult);
            musicTrackService.updateRecognition(track, musicInfo, recognitionResult);

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
    public ResponseEntity<?> recognizeMusic(@RequestParam(value = "uploadId", required = false) Long uploadId,
                                            @RequestParam(value = "filePath", required = false) String filePath,
                                            @RequestParam(value = "useSeparatedVocals", defaultValue = "false") boolean useSeparatedVocals) {
        try {
            MusicTrack track = resolveManagedTrack(uploadId, filePath);
            Path audioFile = fileUploadService.resolveManagedFile(track.getFilePath());

            AuddApiService.MusicInfo musicInfo = null;
            String rawResult = null;
            try {
                rawResult = auddApiService.recognizeAudio(audioFile.toFile());
                musicInfo = auddApiService.parseResult(rawResult);
            } catch (Exception e) {
                System.err.println("Audd 识别失败，尝试使用阿里语音识别: " + e.getMessage());
            }

            if (musicInfo == null || isEmpty(musicInfo.getTitle()) && isEmpty(musicInfo.getArtist()) && isEmpty(musicInfo.getAlbum())) {
                Path asrInput = audioFile;
                if (useSeparatedVocals) {
                    try {
                        Map<String, Object> separation = sourceSeparationService.separate(audioFile, "vocals");
                        Path vocalsStem = resolveStemPath(separation, "vocals");
                        if (vocalsStem != null) {
                            asrInput = vocalsStem;
                        }
                    } catch (Exception e) {
                        System.err.println("Demucs 分轨失败，回退原始音频 ASR: " + e.getMessage());
                    }
                }

                String text = qwenAsrService.transcribeAudio(asrInput.toFile());
                if (text != null && !text.isEmpty()) {
                    if (musicInfo == null) {
                        musicInfo = new AuddApiService.MusicInfo();
                    }
                    musicInfo.setTitle("未知");
                    musicInfo.setArtist("未知");
                    musicInfo.setAlbum("未知");
                    musicInfo.setLyrics(text);
                    musicInfo.setRawJson(text);
                    rawResult = text;
                }
            }

            musicTrackService.updateRecognition(track, musicInfo, rawResult);

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

    /**
     * 基础音乐成分分析
     */
    @PostMapping("/features")
    public ResponseEntity<?> analyzeMusicFeatures(@RequestParam(value = "uploadId", required = false) Long uploadId,
                                                  @RequestParam(value = "filePath", required = false) String filePath,
                                                  @RequestParam(value = "includeStems", defaultValue = "false") boolean includeStems) {
        try {
            MusicTrack track = resolveManagedTrack(uploadId, filePath);
            Path audioFile = fileUploadService.resolveManagedFile(track.getFilePath());
            Path stemsDir = null;
            if (includeStems) {
                Map<String, Object> separation = sourceSeparationService.separate(audioFile);
                stemsDir = resolveOutputDirectory(separation);
            }
            Map<String, Object> features = audioFeatureAnalysisService.analyze(audioFile, stemsDir);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分析成功");
            response.put("data", features);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "分析失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * MERT 语义分析
     */
    @PostMapping("/semantic")
    public ResponseEntity<?> analyzeMusicSemantics(@RequestParam(value = "uploadId", required = false) Long uploadId,
                                                   @RequestParam(value = "filePath", required = false) String filePath) {
        try {
            MusicTrack track = resolveManagedTrack(uploadId, filePath);
            Path audioFile = fileUploadService.resolveManagedFile(track.getFilePath());
            Map<String, Object> semantic = audioSemanticAnalysisService.analyze(audioFile);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分析成功");
            response.put("data", semantic);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "分析失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Demucs 分轨
     */
    @PostMapping("/separate")
    public ResponseEntity<?> separateMusic(@RequestParam(value = "uploadId", required = false) Long uploadId,
                                           @RequestParam(value = "filePath", required = false) String filePath,
                                           @RequestParam(value = "twoStems", required = false) String twoStems) {
        try {
            MusicTrack track = resolveManagedTrack(uploadId, filePath);
            Path audioFile = fileUploadService.resolveManagedFile(track.getFilePath());
            Map<String, Object> separation = sourceSeparationService.separate(audioFile, twoStems);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分轨成功");
            response.put("data", separation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "分轨失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 聚合音乐智能分析：识别 + 分轨 + 特征 + 语义 + GPT 视觉解释
     */
    @PostMapping("/intelligence")
    public ResponseEntity<?> analyzeMusicIntelligence(@RequestParam(value = "uploadId", required = false) Long uploadId,
                                                      @RequestParam(value = "filePath", required = false) String filePath,
                                                      @RequestParam(value = "includeStems", defaultValue = "true") boolean includeStems,
                                                      @RequestParam(value = "includeSemantic", defaultValue = "true") boolean includeSemantic,
                                                      @RequestParam(value = "useSeparatedVocals", defaultValue = "true") boolean useSeparatedVocals) {
        try {
            MusicTrack track = resolveManagedTrack(uploadId, filePath);
            Path audioFile = fileUploadService.resolveManagedFile(track.getFilePath());
            List<String> warnings = new ArrayList<>();

            Map<String, Object> separation = null;
            Path stemsDir = null;
            if (includeStems || useSeparatedVocals) {
                try {
                    separation = sourceSeparationService.separate(audioFile);
                    stemsDir = resolveOutputDirectory(separation);
                } catch (Exception e) {
                    warnings.add("Demucs 分轨失败，已回退原始音频: " + e.getMessage());
                }
            }

            AuddApiService.MusicInfo musicInfo = null;
            String rawResult = null;
            try {
                rawResult = auddApiService.recognizeAudio(audioFile.toFile());
                musicInfo = auddApiService.parseResult(rawResult);
            } catch (Exception e) {
                warnings.add("Audd 识别失败，尝试使用 ASR 回退: " + e.getMessage());
            }

            if (musicInfo == null || isEmpty(musicInfo.getTitle()) && isEmpty(musicInfo.getArtist()) && isEmpty(musicInfo.getAlbum())) {
                Path asrInput = audioFile;
                if (useSeparatedVocals && separation != null) {
                    Path vocalsStem = resolveStemPath(separation, "vocals");
                    if (vocalsStem != null) {
                        asrInput = vocalsStem;
                    }
                }

                try {
                    String text = qwenAsrService.transcribeAudio(asrInput.toFile());
                    if (text != null && !text.isEmpty()) {
                        if (musicInfo == null) {
                            musicInfo = new AuddApiService.MusicInfo();
                        }
                        musicInfo.setTitle("未知");
                        musicInfo.setArtist("未知");
                        musicInfo.setAlbum("未知");
                        musicInfo.setLyrics(text);
                        musicInfo.setRawJson(text);
                        rawResult = text;
                    }
                } catch (Exception e) {
                    warnings.add("ASR 回退失败: " + e.getMessage());
                }
            }

            musicTrackService.updateRecognition(track, musicInfo, rawResult);

            Map<String, Object> featureInfo = null;
            try {
                featureInfo = audioFeatureAnalysisService.analyze(audioFile, includeStems ? stemsDir : null);
            } catch (Exception e) {
                warnings.add("音乐特征分析失败: " + e.getMessage());
            }

            Map<String, Object> semanticInfo = null;
            if (includeSemantic) {
                try {
                    semanticInfo = audioSemanticAnalysisService.analyze(audioFile);
                } catch (Exception e) {
                    warnings.add("音乐语义分析失败: " + e.getMessage());
                }
            }

            Object aiAnalysis = null;
            try {
                aiAnalysis = musicDesignAnalysisService.analyze(
                        musicInfo != null ? musicInfo.getTitle() : "未知",
                        musicInfo != null ? musicInfo.getArtist() : "未知",
                        musicInfo != null ? musicInfo.getAlbum() : "未知",
                        musicInfo != null ? musicInfo.getLyrics() : "",
                        featureInfo,
                        semanticInfo,
                        null,
                        null
                );
            } catch (Exception e) {
                warnings.add("GPT 视觉解释失败: " + e.getMessage());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("uploadId", track.getId());
            data.put("musicInfo", musicInfo);
            data.put("audioFeatures", featureInfo);
            data.put("semanticProfile", semanticInfo);
            data.put("aiAnalysis", aiAnalysis);
            data.put("sourceSeparation", separation);
            data.put("warnings", warnings);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", warnings.isEmpty() ? "聚合分析成功" : "聚合分析完成（含部分降级）");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "聚合分析失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private MusicTrack resolveManagedTrack(Long uploadId, String filePath) {
        MusicTrack track;
        if (uploadId != null) {
            track = musicTrackService.getTrackById(uploadId);
        } else if (!isEmpty(filePath)) {
            String normalizedPath = fileUploadService.normalizeManagedPath(filePath);
            track = musicTrackService.findByFilePath(normalizedPath)
                    .orElseThrow(() -> new RuntimeException("无法识别未登记的上传文件"));
        } else {
            throw new RuntimeException("缺少 uploadId 或兼容 filePath 参数");
        }

        if (!"TRACK".equals(track.getRecordType()) || isEmpty(track.getFilePath())) {
            throw new RuntimeException("仅支持识别已上传的音频记录");
        }
        return track;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Path resolveStemPath(Map<String, Object> separation, String stemName) {
        if (separation == null) {
            return null;
        }
        Object stemsObject = separation.get("stems");
        if (!(stemsObject instanceof Map<?, ?> stems)) {
            return null;
        }
        Object stemPath = stems.get(stemName);
        if (!(stemPath instanceof String stemPathValue) || stemPathValue.isBlank()) {
            return null;
        }
        return Path.of(stemPathValue).toAbsolutePath().normalize();
    }

    private Path resolveOutputDirectory(Map<String, Object> separation) {
        Object outputDir = separation.get("output_dir");
        if (!(outputDir instanceof String outputDirValue) || outputDirValue.isBlank()) {
            return null;
        }
        return Path.of(outputDirValue).toAbsolutePath().normalize();
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
