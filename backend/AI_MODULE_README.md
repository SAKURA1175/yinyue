# AI 图像生成模块使用说明

## 1. 模块简介
本模块提供基于 Stable Diffusion 的文本到图像生成功能，支持自定义提示词、图像尺寸、采样步数、高清修复等高级参数。旨在为音乐专辑封面生成提供高质量的视觉素材。

## 2. 接口文档

### 2.1 生成图片

**接口地址**: `/ai/generate-image`
**请求方式**: `POST`
**Content-Type**: `application/json`

**请求参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `prompt` | String | 是 | - | 正向提示词，描述想要生成的画面 |
| `negative_prompt` | String | 否 | "" | 反向提示词，描述不希望出现的元素 |
| `width` | Integer | 否 | 512 | 图片宽度 |
| `height` | Integer | 否 | 512 | 图片高度 |
| `steps` | Integer | 否 | 20 | 迭代步数，越高细节越丰富但耗时越长 |
| `cfg_scale` | Double | 否 | 7.0 | 提示词相关性，越高越忠实于提示词 |
| `sampler_name` | String | 否 | "Euler a" | 采样器名称 (如 "Euler", "DPM++ 2M Karras") |
| `seed` | Long | 否 | -1 | 随机种子，-1 为随机 |
| `enable_hires` | Boolean | 否 | false | 是否启用高清修复 (Hires. fix) |
| `hr_scale` | Double | 否 | 2.0 | 高清修复放大倍数 |
| `hr_upscaler` | String | 否 | "Latent" | 放大算法 (如 "Latent", "ESRGAN_4x") |
| `denoising_strength`| Double | 否 | 0.7 | 重绘幅度 (0-1)，用于高清修复 |

**请求示例**:

```json
{
  "prompt": "a beautiful landscape, mountains, lake, sunset, 8k, best quality",
  "negative_prompt": "blurry, low quality, text, watermark",
  "width": 512,
  "height": 512,
  "steps": 25,
  "cfg_scale": 7.5,
  "sampler_name": "DPM++ 2M Karras",
  "enable_hires": true,
  "hr_scale": 1.5,
  "denoising_strength": 0.5
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "生成成功",
  "data": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
}
```
*注意：`data` 字段为生成的图片 Base64 编码字符串。*

## 3. 配置说明

在 `application.yml` 中配置 Stable Diffusion WebUI 的 API 地址：

```yaml
app:
  ai:
    stable-diffusion:
      endpoint: http://127.0.0.1:7860  # SD WebUI 的地址
```

## 4. 依赖说明
本模块依赖本地运行的 Stable Diffusion WebUI，并且需要开启 API 模式（启动参数增加 `--api`）。

## 5. 错误处理
- 如果 API 无法连接，将返回 500 错误。
- 如果 Prompt 为空，返回 400 错误。
- 所有异常都会被捕获并记录日志。
