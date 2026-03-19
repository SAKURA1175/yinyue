# 参考封面检索增强（两周 MVP）实施规格

## 1. 目标

在现有“音乐分析 -> prompt -> Stable Diffusion”链路上增加“参考检索 + 约束生成”能力：

- 让生成封面更接近主流乐队专辑的专业质感
- 通过约束层避免直接复刻具体专辑元素
- 形成可灰度上线、可评估、可回退的工程闭环

## 2. 架构与数据流

1. 上传与分析完成后，得到：
   - 结构化特征（Essentia/librosa）
   - 语义标签与 embedding（MERT）
2. 生成前调用 `POST /api/reference/search`
3. 检索服务从向量库召回 topK 参考封面并重排
4. 后端将参考结果转为 `styleConstraints`
5. 调用 `POST /api/generate/with-reference`
6. Qwen 生成 `final_prompt + negative_prompt`
7. Stable Diffusion 生图（多候选）
8. 输出结果并记录审计与评分

## 3. 数据模型（MySQL）

以下是 MVP 期建议新增的普通数据库表。

```sql
CREATE TABLE cover_reference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  artist VARCHAR(255) NOT NULL,
  release_year INT NULL,
  genre VARCHAR(100) NULL,
  mood_tags JSON NULL,
  style_tags JSON NULL,
  color_palette JSON NULL,
  composition_tags JSON NULL,
  source_url VARCHAR(512) NULL,
  image_storage_key VARCHAR(512) NOT NULL,
  license_tag VARCHAR(50) NOT NULL DEFAULT 'unknown',
  is_enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_cover_reference_genre (genre),
  INDEX idx_cover_reference_enabled (is_enabled),
  INDEX idx_cover_reference_year (release_year)
);

CREATE TABLE reference_query_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  upload_id BIGINT NULL,
  request_json JSON NOT NULL,
  topk INT NOT NULL,
  latency_ms INT NOT NULL,
  result_ids JSON NOT NULL,
  risk_flags JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_reference_query_log_upload (upload_id),
  INDEX idx_reference_query_log_created (created_at)
);

CREATE TABLE generation_risk_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  generation_record_id BIGINT NULL,
  upload_id BIGINT NULL,
  reference_ids JSON NULL,
  max_similarity_score DECIMAL(6,4) NULL,
  risk_level VARCHAR(20) NOT NULL,
  hit_rules JSON NULL,
  action_taken VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_generation_risk_upload (upload_id),
  INDEX idx_generation_risk_level (risk_level),
  INDEX idx_generation_risk_created (created_at)
);
```

## 4. 向量库集合设计

MVP 建议仅 1 个集合：`cover_reference_vectors`

- `id`：与 `cover_reference.id` 对齐
- `vector`：图像 embedding（建议 512/768 维）
- `payload`：
  - `genre`
  - `release_year`
  - `mood_tags`
  - `style_tags`
  - `license_tag`
  - `is_enabled`

检索流程：

1. payload 过滤（`is_enabled=true` + 可选 `genre/year/license`）
2. 向量近邻召回 topK=20
3. 规则重排输出 topN=5

## 5. API 契约草案

### 5.1 `POST /api/reference/search`

用途：根据音乐分析结果召回参考封面方向。

请求：

```json
{
  "uploadId": 123,
  "intentText": "做一张偏冷色、有冲击力的摇滚封面",
  "audioProfile": {
    "rhythm": { "bpm": 132.0 },
    "tonal": { "key": "A", "scale": "minor" },
    "energy": { "level": "high" }
  },
  "semanticProfile": {
    "mood": [{ "label": "energetic", "score": 0.82 }],
    "genre": [{ "label": "rock", "score": 0.78 }],
    "tags": [{ "label": "dark", "score": 0.71 }]
  },
  "topK": 20,
  "topN": 5
}
```

响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "querySummary": {
      "targetGenre": "rock",
      "targetMood": ["energetic", "dark"],
      "targetEra": "any"
    },
    "references": [
      {
        "id": 9001,
        "title": "reference-1",
        "artist": "band-a",
        "score": 0.872,
        "genre": "rock",
        "moodTags": ["dark", "intense"],
        "styleTags": ["high-contrast", "gritty"],
        "colorPalette": ["#0F1115", "#8A1C1C", "#C7C7C7"]
      }
    ],
    "styleConstraints": {
      "visualDirection": "dark energetic rock",
      "paletteHints": ["deep black", "desaturated red", "steel gray"],
      "compositionHints": ["center focus", "strong diagonal motion"],
      "textureHints": ["grain", "rough print"],
      "forbiddenElements": [
        "band logo reproduction",
        "album title reproduction",
        "iconic character copy"
      ]
    }
  }
}
```

### 5.2 `POST /api/generate/with-reference`

用途：带参考约束进行封面生成。

请求：

```json
{
  "uploadId": 123,
  "userIntent": "做一张更有电影感的封面",
  "analysis": {
    "theme": "都市夜行",
    "mood": "压迫且热烈",
    "visual_style": "cinematic"
  },
  "styleConstraints": {
    "visualDirection": "dark energetic rock",
    "paletteHints": ["deep black", "desaturated red", "steel gray"],
    "compositionHints": ["center focus", "strong diagonal motion"],
    "forbiddenElements": ["logo", "title copy"]
  },
  "references": [9001, 9002, 9008],
  "imageOptions": {
    "width": 768,
    "height": 768,
    "steps": 30,
    "batch": 4
  }
}
```

响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "finalPrompt": "...",
    "negativePrompt": "...",
    "candidates": [
      { "imageBase64": "...", "score": 0.83, "riskLevel": "low" }
    ],
    "selected": { "index": 0, "reason": "best style match" }
  }
}
```

## 6. styleConstraints JSON Schema（MVP）

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "StyleConstraints",
  "type": "object",
  "required": ["visualDirection", "forbiddenElements"],
  "properties": {
    "visualDirection": { "type": "string", "minLength": 3 },
    "paletteHints": {
      "type": "array",
      "items": { "type": "string" },
      "maxItems": 8
    },
    "compositionHints": {
      "type": "array",
      "items": { "type": "string" },
      "maxItems": 8
    },
    "textureHints": {
      "type": "array",
      "items": { "type": "string" },
      "maxItems": 8
    },
    "forbiddenElements": {
      "type": "array",
      "items": { "type": "string" },
      "minItems": 1,
      "maxItems": 20
    }
  },
  "additionalProperties": false
}
```

## 7. 与现有链路的对接点

- 在 `POST /api/ai/analyze` 前插入 `POST /api/reference/search`
- `AIController` 增加 `referenceHints/styleConstraints` 入参支持
- `generate-image` 增强为“多候选生成 + 风险打分 + 选择最佳”
- 现有 `/api/music/features`、`/api/music/semantic` 输出作为检索条件直接复用

## 8. 风险控制（MVP 必做）

1. 禁止词与禁用元素写入 `negative_prompt`
2. 生成后对候选图做“与参考图相似度”审查，超过阈值自动降级或重生
3. 对 `license_tag` 非允许数据源默认降权或过滤
4. 保留审计日志，支持问题样本回溯

## 9. 两周任务拆解

### 第 1 周

1. 建表与基础迁移
2. 向量入库脚本（离线）
3. `POST /api/reference/search` 最小可用实现
4. 检索结果转 `styleConstraints`

### 第 2 周

1. `POST /api/generate/with-reference` 接口
2. Qwen 约束提示词模板
3. 候选图风险打分与自动回退
4. 小流量灰度与指标看板接入

## 10. 上线验收指标

- 检索相关性人工评分 >= 3.8/5
- 生成结果用户满意度较基线提升 >= 15%
- 高风险相似度命中率 <= 2%
- 接口 p95：
  - `/api/reference/search` <= 600ms
  - `/api/generate/with-reference` 维持现有生成 SLA ±15%
