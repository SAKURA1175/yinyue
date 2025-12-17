# 网易云链接导入功能

## 功能概述

用户可以通过粘贴网易云音乐链接，直接导入音乐信息，系统将在上传界面展示解析结果。

## 使用流程

### 前端交互流程

1. **Hero 组件**（首页）
   - 用户点击"网易云链接"选项卡
   - 输入网易云链接（如：`https://music.163.com/song?id=1415945617`）
   - 点击"从链接导入 →"按钮

2. **API 调用**
   - Hero 组件调用 `POST /api/ai/netease/parse`
   - 传递网易云链接 URL
   - 显示"解析中..."加载状态

3. **数据传递**
   - 解析成功后，调用 `onNeteaseImportSuccess` 回调函数
   - 将解析的音乐信息传递给父组件（App.jsx）
   - 自动滚动到上传界面

4. **Upload 组件展示**
   - 显示"网易云链接解析结果"卡片，包含：
     - **专辑封面**：如果有则显示图片，否则显示占位符
     - **音乐标题**：解析得到的歌名
     - **艺术家**：歌手名称
     - **专辑**：专辑名称
     - **类型**：歌曲/专辑/歌单
   - 显示"已从网易云链接导入"提示条
   - 用户可点击"×"按钮清除网易云数据并重新选择

## 技术实现

### 后端 API

**端点**: `POST /api/ai/netease/parse`

**请求格式**:
```json
{
  "url": "https://music.163.com/song?id=1415945617"
}
```

**响应格式**:
```json
{
  "code": 200,
  "message": "解析成功",
  "data": {
    "id": "1415945617",
    "title": "网易云音乐",
    "artist": "未知歌手",
    "album": "未知专辑",
    "type": "song",
    "cover_url": "",
    "lyrics": ""
  }
}
```

### 前端组件数据流

```
Hero.jsx (状态: neteaseUrl, neteaseLoading, neteaseError)
    ↓
    调用 onNeteaseImportSuccess(musicData)
    ↓
App.jsx (状态: neteaseData, handleNeteaseImportSuccess)
    ↓
Upload.jsx (Props: neteaseData, onClearNetease)
    ↓
    显示网易云信息卡片
```

### 样式类

#### 结果卡片样式
- `.netease-result-card`: 主卡片容器，带渐变背景和动画
- `.netease-result-header`: 卡片头部，包含标题和关闭按钮
- `.netease-result-content`: 卡片内容区，包含封面和信息
- `.netease-cover`: 专辑封面容器（120x120px）
- `.netease-info`: 音乐信息网格（2列布局）
- `.info-item`: 单个信息项（标签 + 数值）

#### 提示条样式
- `.netease-selected-hint`: 已选中音乐提示条
- `.btn-close`: 关闭按钮

## 文件修改清单

### 后端
- `AIController.java`
  - 添加 `@PostMapping("/netease/parse")` 方法
  - 添加 URL ID 提取方法
  - 添加 URL 类型识别方法
  - 添加音乐信息获取方法（当前返回模拟数据）

### 前端
- `App.jsx`
  - 添加 `neteaseData` 状态
  - 添加 `handleNeteaseImportSuccess` 回调函数
  - 传递 `onNeteaseImportSuccess` 到 Hero 组件
  - 传递 `neteaseData` 和 `onClearNetease` 到 Upload 组件

- `sections/Hero.jsx`
  - 添加 `onNeteaseImportSuccess` 参数
  - 修改 `handleNeteaseImport` 以调用该回调

- `sections/Upload.jsx`
  - 添加 `neteaseData` 和 `onClearNetease` 参数
  - 添加网易云结果卡片 JSX
  - 添加已选中提示条 JSX

- `sections/Upload.css`
  - 添加网易云卡片相关样式
  - 添加响应式设计（移动端适配）

## 后续改进

### 真实 API 集成
当前实现返回模拟数据，未来可以：
1. 集成网易云音乐 API（如需公开）
2. 通过爬虫获取音乐信息（需遵守法律法规）
3. 本地数据库缓存常用歌曲信息

### 功能扩展
1. 支持更多音乐平台（QQ音乐、酷狗、咪咕等）
2. 添加音乐信息编辑功能
3. 记住最近导入的歌曲
4. 批量导入歌曲列表

## 测试方法

### 单元测试（后端）
```bash
curl -X POST http://localhost:8080/api/ai/netease/parse \
  -H "Content-Type: application/json" \
  -d '{"url":"https://music.163.com/song?id=1415945617"}'
```

### 集成测试（前端）
1. 打开浏览器访问 `http://localhost:5173`
2. 点击首页"网易云链接"选项卡
3. 粘贴网易云链接
4. 点击"从链接导入 →"
5. 确认自动滚动到上传界面
6. 验证显示解析结果卡片和提示条
7. 测试关闭按钮功能
8. 测试上传音乐文件功能
