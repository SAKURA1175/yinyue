package com.yinyue.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yinyue.ai.llm.QwenLLMService;
import com.yinyue.dto.AnalysisResultData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MusicDesignAnalysisService {

    private final QwenLLMService qwenLLMService;
    private final Gson gson = new Gson();

    public MusicDesignAnalysisService(QwenLLMService qwenLLMService) {
        this.qwenLLMService = qwenLLMService;
    }

    public AnalysisResultData analyze(String title,
                                      String artist,
                                      String album,
                                      String lyrics,
                                      Object audioProfile,
                                      Object semanticProfile,
                                      Object referenceHints,
                                      Object styleConstraints) {
        String prompt = buildPrompt(
                readString(title, "未知"),
                readString(artist, "未知"),
                readString(album, "未知"),
                readString(lyrics, ""),
                audioProfile,
                semanticProfile,
                referenceHints,
                styleConstraints
        );

        String analysisResult = qwenLLMService.callQwenLLM(prompt);
        return parseAnalysisResult(analysisResult);
    }

    private String buildPrompt(String title,
                               String artist,
                               String album,
                               String lyrics,
                               Object audioProfile,
                               Object semanticProfile,
                               Object referenceHints,
                               Object styleConstraints) {
        String audioSummary = buildAudioSummary(audioProfile);
        String semanticSummary = buildSemanticSummary(semanticProfile);
        String referenceSummary = buildReferenceSummary(referenceHints);
        String styleConstraintsSummary = buildStyleConstraintsSummary(styleConstraints);

        return String.format(
                "你是一个专业的音乐分析师和视觉设计顾问。你的任务是根据音乐信息，输出一份适合生成专辑封面的结构化分析。\n\n" +
                "【音乐信息】\n" +
                "歌名：%s\n" +
                "歌手：%s\n" +
                "专辑：%s\n" +
                "%s\n" +
                "%s\n" +
                "%s\n" +
                "%s\n" +
                "%s\n\n" +
                "【重要指令】\n" +
                "1. 如果信息不完整，不要直接返回“未知”，而是基于已有线索给出合理的艺术化推断。\n" +
                "2. 所有字段都必须填写具体、生动、可执行的内容。\n" +
                "3. image_prompt_en 必须是高质量 Stable Diffusion 英文提示词，包含主体、构图、光影、材质、风格。\n" +
                "4. colors 必须返回 3 到 5 个十六进制颜色值。\n\n" +
                "请严格输出 JSON，不要包含 Markdown 代码块：\n" +
                "{\n" +
                "  \"theme\": \"核心主题\",\n" +
                "  \"mood\": \"情感氛围\",\n" +
                "  \"visual_style\": \"视觉风格\",\n" +
                "  \"colors\": [\"#HexColor1\", \"#HexColor2\", \"#HexColor3\"],\n" +
                "  \"summary\": \"对音乐成分和整体气质的中文解读\",\n" +
                "  \"cover_concept\": \"专辑封面的主画面概念\",\n" +
                "  \"design_rationale\": \"为什么这样的视觉语言适合这首歌\",\n" +
                "  \"prompt_notes\": [\"用于生图的补充约束1\", \"补充约束2\"],\n" +
                "  \"image_prompt_en\": \"(英文) masterpiece, best quality, album cover, ...\"\n" +
                "}",
                title,
                artist,
                album,
                lyrics.isEmpty() ? "" : "歌词片段：" + truncate(lyrics, 500),
                audioSummary,
                semanticSummary,
                referenceSummary,
                styleConstraintsSummary
        );
    }

    public AnalysisResultData parseAnalysisResult(String jsonString) {
        AnalysisResultData result = new AnalysisResultData();

        try {
            String sanitized = sanitizeJson(jsonString);
            Map<String, Object> parsed = gson.fromJson(sanitized, new TypeToken<Map<String, Object>>() {}.getType());
            if (parsed == null) {
                throw new IllegalArgumentException("AI 返回内容为空");
            }

            result.setTheme(readString(parsed.get("theme"), "未知"));
            result.setMood(readString(parsed.get("mood"), "未知"));
            result.setVisualStyle(readString(parsed.get("visual_style"), "未知"));
            result.setSummary(readString(parsed.get("summary"), "这首歌呈现出鲜明但尚未完全确定的音乐气质。"));
            result.setCoverConcept(readString(parsed.get("cover_concept"), "以音乐氛围为核心的概念型封面。"));
            result.setDesignRationale(readString(parsed.get("design_rationale"), "视觉方案会围绕歌曲的节奏、调性和情绪展开。"));

            Object colorsObj = parsed.get("colors");
            if (colorsObj instanceof List<?> colorList && !colorList.isEmpty()) {
                result.setColors(colorList.stream().map(String::valueOf).toArray(String[]::new));
            } else {
                result.setColors(new String[]{"#D93C39", "#FF6565", "#FFB3B0"});
            }

            Object promptNotesObj = parsed.get("prompt_notes");
            if (promptNotesObj instanceof List<?> promptNotes && !promptNotes.isEmpty()) {
                result.setPromptNotes(promptNotes.stream().map(String::valueOf).toArray(String[]::new));
            } else {
                result.setPromptNotes(new String[0]);
            }

            String prompt = readString(parsed.get("image_prompt_en"), "");
            if (prompt.isEmpty()) {
                prompt = readString(parsed.get("prompt"), "");
            }
            result.setImagePromptEn(prompt.isEmpty() ? "Generate a professional album cover" : prompt);
            return result;
        } catch (Exception e) {
            System.err.println("AI 分析结果解析失败，回退默认值: " + e.getMessage());
            result.setTheme("未知");
            result.setMood("未知");
            result.setVisualStyle("未知");
            result.setColors(new String[]{"#D93C39", "#FF6565", "#FFB3B0"});
            result.setSummary("当前未能稳定解析 AI 响应，已回退到默认说明。");
            result.setCoverConcept("概念化的音乐封面，以情绪和色彩为中心。");
            result.setDesignRationale("优先保留可生成的视觉方向，避免中断主流程。");
            result.setPromptNotes(new String[0]);
            result.setImagePromptEn("Generate a professional album cover");
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private String buildAudioSummary(Object audioProfileObject) {
        if (!(audioProfileObject instanceof Map<?, ?> audioProfile)) {
            return "";
        }

        Object rhythmObject = audioProfile.get("rhythm");
        Object tonalObject = audioProfile.get("tonal");
        Object danceabilityObject = audioProfile.get("danceability");
        Object energyObject = audioProfile.get("energy");
        Object chordsObject = audioProfile.get("chords");

        StringBuilder builder = new StringBuilder("音频结构化特征：");
        boolean hasContent = false;

        if (rhythmObject instanceof Map<?, ?> rhythm) {
            Object bpm = rhythm.get("bpm");
            if (bpm != null) {
                builder.append("BPM=").append(bpm).append("；");
                hasContent = true;
            }
        }
        if (tonalObject instanceof Map<?, ?> tonal) {
            Object key = tonal.get("key");
            Object scale = tonal.get("scale");
            if (key != null) {
                builder.append("调性=").append(key);
                if (scale != null) {
                    builder.append(" ").append(scale);
                }
                builder.append("；");
                hasContent = true;
            }
        }
        if (danceabilityObject instanceof Map<?, ?> danceability) {
            Object score = danceability.get("score");
            if (score != null) {
                builder.append("舞动感=").append(score).append("；");
                hasContent = true;
            }
        }
        if (energyObject instanceof Map<?, ?> energy) {
            Object level = energy.get("level");
            if (level != null) {
                builder.append("能量等级=").append(level).append("；");
                hasContent = true;
            }
        }
        if (chordsObject instanceof Map<?, ?> chords && chords.get("histogram") instanceof List<?> histogram && !histogram.isEmpty()) {
            String chordLabels = histogram.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> item.get("label"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .limit(4)
                    .collect(Collectors.joining(", "));
            if (!chordLabels.isBlank()) {
                builder.append("常见和弦=").append(chordLabels).append("；");
                hasContent = true;
            }
        }

        return hasContent ? builder.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private String buildSemanticSummary(Object semanticProfileObject) {
        if (!(semanticProfileObject instanceof Map<?, ?> semanticProfile)) {
            return "";
        }

        String moods = joinLabels(semanticProfile.get("mood"));
        String genres = joinLabels(semanticProfile.get("genre"));
        String tags = joinLabels(semanticProfile.get("tags"));

        if (moods.isEmpty() && genres.isEmpty() && tags.isEmpty()) {
            return "";
        }

        return String.format("音乐语义标签：情绪=%s；风格=%s；标签=%s",
                emptyAsUnknown(moods),
                emptyAsUnknown(genres),
                emptyAsUnknown(tags));
    }

    @SuppressWarnings("unchecked")
    private String joinLabels(Object labelsObject) {
        if (!(labelsObject instanceof List<?> labels)) {
            return "";
        }

        return labels.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> item.get("label"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private String buildReferenceSummary(Object referenceHintsObject) {
        if (!(referenceHintsObject instanceof List<?> references) || references.isEmpty()) {
            return "";
        }

        String referencesText = references.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> readString(item.get("title"), "") + " by " + readString(item.get("artist"), ""))
                .filter(value -> !value.equals(" by "))
                .limit(5)
                .collect(Collectors.joining("; "));

        return referencesText.isBlank() ? "" : "参考方向：" + referencesText;
    }

    @SuppressWarnings("unchecked")
    private String buildStyleConstraintsSummary(Object constraintsObject) {
        if (!(constraintsObject instanceof Map<?, ?> constraints)) {
            return "";
        }

        String visualDirection = readString(constraints.get("visualDirection"), "");
        String paletteHints = joinSimpleList(constraints.get("paletteHints"));
        String compositionHints = joinSimpleList(constraints.get("compositionHints"));
        String forbiddenElements = joinSimpleList(constraints.get("forbiddenElements"));

        if (visualDirection.isBlank() && paletteHints.isBlank() && compositionHints.isBlank() && forbiddenElements.isBlank()) {
            return "";
        }

        return String.format("视觉约束：方向=%s；配色=%s；构图=%s；禁用=%s",
                emptyAsUnknown(visualDirection),
                emptyAsUnknown(paletteHints),
                emptyAsUnknown(compositionHints),
                emptyAsUnknown(forbiddenElements));
    }

    @SuppressWarnings("unchecked")
    private String joinSimpleList(Object listObject) {
        if (!(listObject instanceof List<?> list)) {
            return "";
        }

        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.joining(", "));
    }

    private String sanitizeJson(String response) {
        if (response == null) {
            return "";
        }

        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    private String readString(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String emptyAsUnknown(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }
}
