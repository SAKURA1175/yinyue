# 音乐 AI 流水线说明

## 结论先说

如果要做“上传歌曲 -> 分析音乐成分 -> 输出报告/生成封面”的系统，`Demucs + Essentia + MERT + Qwen2-Audio` 是一条合理组合，但它们各自负责的事情并不一样：

- `Demucs` 负责“拆”
- `Essentia` 负责“算”
- `MERT` 负责“理解”
- `Qwen2-Audio` 负责“说”

它们不是同一类模型，不能互相替代。

最常见的误区有两个：

- 把 `Qwen2-Audio` 当成高精度 BPM / 调性 / 和弦分析器
- 把 `MERT` 当成开箱即用的节奏和和声检测器

这两种理解都不准确。

## 四个模块分别做什么

### Demucs

`Demucs` 是音乐源分离模型，主要作用是把完整歌曲拆成若干 stem。

常见输出包括：

- `vocals`
- `drums`
- `bass`
- `other`

有些模型和分支还能进一步拆出：

- `guitar`
- `piano`

它适合解决的问题：

- 人声、鼓点、低频和伴奏混在一起，导致后续分析不稳定
- 想单独分析鼓组能量、人声内容或贝斯走向
- 想做伴奏提取、karaoke、ASR 人声增强

它不适合做的事情：

- 直接判断这首歌的流派和情绪
- 直接输出 BPM、调性、和弦进行
- 直接生成解释性文本

项目里可以把它理解成“音频预处理手术刀”，不是最终决策模型。

### Essentia

`Essentia` 是音乐信息检索（MIR）工具箱，本质上是专业的音频分析引擎。

它最擅长把音乐转换成结构化特征，例如：

- `tempo / BPM`
- `beat / onset`
- `key`
- `chords`
- `loudness`
- `danceability`
- `tonal descriptors`
- 部分高层标签，如 `mood / genre / instrumentation`

它适合的任务：

- 做核心音乐成分分析
- 生成可入库、可排序、可展示的 JSON 特征
- 给前端展示仪表化结果
- 给后续 LLM 提供稳定、可解释的输入

它不适合的任务：

- 直接产出自然语言报告
- 直接做音频对话
- 替代视觉或文本大模型

在这套系统里，`Essentia` 是最接近“测量仪器”的部分。

### MERT

`MERT` 是音乐表征模型，也可以理解为音乐 embedding 编码器。

它的核心输出不是歌词，不是 BPM，也不是和弦，而是一个高维向量。这个向量表示的是音乐的高层语义特征，例如：

- 风格倾向
- 情绪倾向
- 音色气质
- 结构整体感
- 音乐之间的相似性

它适合的任务：

- 风格分类
- 情绪标签分类
- 乐器标签分类
- 音乐相似度检索
- 聚类与推荐
- 下游分类头特征输入

它不适合的任务：

- 直接输出精准 BPM
- 直接输出和弦进行
- 直接写解释文本

所以 `MERT` 的定位更像“音乐理解 backbone”，而不是终端分析器。

### Qwen2-Audio

`Qwen2-Audio` 是音频语言模型，适合把音频内容或分析结果转换成语言描述。

它的强项包括：

- 音频问答
- 音频描述
- 跨模态理解
- 基于音频或结构化结果生成解释文本

它适合做的事情：

- “这段音乐大概是什么风格？”
- “这段音乐听起来有哪些乐器？”
- “请把这段音乐描述成封面设计提示词”
- “根据分析结果写一段可读报告”

它不适合做的事情：

- 替代专业 MIR 工具做高精度 BPM / key / chord 检测
- 长音乐的底层精确分析

如果前面已经有 `Essentia + MERT` 这类结构化结果，`Qwen2-Audio` 更适合作为解释层，而不是底层分析层。

## 四者如何协同工作

一条合理的流水线如下：

1. 上传歌曲
2. `Demucs` 分 stem
3. `Essentia` 对原曲和 stem 分别提取结构化特征
4. `MERT` 抽 embedding，补风格、情绪、相似度等高层语义
5. 将结果合并成统一 JSON
6. `Qwen2-Audio` 或普通文本 LLM 生成解释文本、封面 prompt、推荐标签

一个典型输出可以长这样：

```json
{
  "tempo": 128,
  "key": "A minor",
  "chords": ["Am", "F", "C", "G"],
  "stems": ["vocals", "drums", "bass", "other"],
  "instrument_tags": ["synth", "kick", "female vocal"],
  "mood_tags": ["melancholic", "energetic", "dreamy"],
  "structure_hint": ["intro", "verse", "chorus", "bridge"],
  "llm_summary": "这首歌整体是电子流行..."
}
```

## 在本项目里的映射

当前项目已经把这条思路落成了可运行主线，但解释层目前默认使用的是通用 `OpenAI` 文本模型，而不是 `Qwen2-Audio`。

对应关系如下：

- `Demucs`
  - 脚本：[separate_audio.py](D:/yinyue/scripts/separate_audio.py)
  - 服务：[SourceSeparationService.java](D:/yinyue/backend/src/main/java/com/yinyue/service/SourceSeparationService.java)
  - 接口：`POST /api/music/separate`

- `Essentia / librosa`
  - 脚本：[analyze_audio.py](D:/yinyue/scripts/analyze_audio.py)
  - 服务：[AudioFeatureAnalysisService.java](D:/yinyue/backend/src/main/java/com/yinyue/service/AudioFeatureAnalysisService.java)
  - 接口：`POST /api/music/features`
  - Windows 本地默认 `librosa`，Docker helper 或容器环境可切到 `Essentia`

- `MERT`
  - 脚本：[extract_music_semantics.py](D:/yinyue/scripts/extract_music_semantics.py)
  - 服务：[AudioSemanticAnalysisService.java](D:/yinyue/backend/src/main/java/com/yinyue/service/AudioSemanticAnalysisService.java)
  - 接口：`POST /api/music/semantic`

- `OpenAI` 文本模型
  - 服务：[MusicDesignAnalysisService.java](D:/yinyue/backend/src/main/java/com/yinyue/service/MusicDesignAnalysisService.java)
  - 当前代码里仍复用历史命名的 LLM 服务：[QwenLLMService.java](D:/yinyue/backend/src/main/java/com/yinyue/ai/llm/QwenLLMService.java)
  - 项目方向上应将这层理解为待统一命名的 `OpenAI` 文本模型服务
  - 接口：`POST /api/ai/analyze`

- 聚合主线
  - 接口：`POST /api/music/intelligence`
  - 作用：串起识别、分轨、结构化分析、语义分析和 GPT 解释

## 当前项目为什么没有默认上 Qwen2-Audio

当前主线没有把 `Qwen2-Audio` 作为默认解释层，主要是出于工程取舍：

- 前面已经有结构化特征和语义标签，解释层更适合通用 `OpenAI` 文本 LLM
- 文本模型在提示词控制、成本、部署和兼容 OpenAI 协议上更顺手
- `Qwen2-Audio` 更适合“直接听音频问答”，而不是重复处理已经结构化的输入

换句话说：

- 如果输入是“原始音频”，`Qwen2-Audio` 很有价值
- 如果输入已经是“特征 + 语义 + 标签 + prompt 约束”，普通文本 LLM 往往更实用

## 推荐的分阶段接入顺序

如果项目要继续演进，建议还是按下面顺序推进：

### 第一阶段

先接 `Essentia`

原因：

- 最快产出真正有价值的音乐成分指标
- 最适合做展示和数据库沉淀

### 第二阶段

再加 `Demucs`

原因：

- 让分析更细
- 特别适合鼓、人声、贝斯这些容易互相干扰的成分

### 第三阶段

再加 `MERT`

原因：

- 补齐风格、情绪、相似度和高层语义
- 为检索、推荐、标签体系打基础

### 第四阶段

最后再评估 `Qwen2-Audio`

原因：

- 它更适合把结果做成产品化解释能力
- 不是底层 MIR 的替代品

## 一句实用判断

- 要“测量音乐”用 `Essentia`
- 要“拆开音乐”用 `Demucs`
- 要“表示音乐”用 `MERT`
- 要“解释音乐”用 `Qwen2-Audio`

## 当前建议

对本项目当前状态，最稳的默认方案仍然是：

- `Demucs`
- `Essentia`
- `MERT`
- `OpenAI` 文本 LLM 解释层

如果后面要做“直接听音频问答”“音频到文案”“短音频交互式解释”，再单独把 `Qwen2-Audio` 接进来会更合适。
