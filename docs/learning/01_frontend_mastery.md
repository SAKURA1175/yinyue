# 🎨 01. 前端觉醒：React 与数据交互

前端是用户看到的"脸面"。在本项目中，前端负责三件事：
1.  **展示**：漂亮的紫色界面。
2.  **交互**：点击上传按钮、点击播放按钮。
3.  **通信**：把文件发给后端，把后端给的图片展示出来。

---

## 1. 核心概念：组件 (Component)

**理论**：
React 把网页拆成一块一块的积木，叫"组件"。一个组件就是一个 `.jsx` 文件。

**项目实战**：
打开 `frontend/src/App.jsx`。
你会发现它像搭积木一样使用了其他组件：
```jsx
// App.jsx (简化版)
function App() {
  return (
    <div className="app-container">
      <Hero />           {/* 顶部大标题区域 */}
      <UploadSection />  {/* 上传按钮区域 */}
      <Analysis />       {/* 歌词分析结果区域 */}
      <Generate />       {/* 图片生成结果区域 */}
    </div>
  )
}
```
**练习**：
试着把 `<UploadSection />` 和 `<Hero />` 换个位置，保存文件，看看浏览器里发生了什么？

---

## 2. 核心概念：状态 (State) - `useState`

**理论**：
网页怎么知道"现在该显示上传按钮"还是"显示进度条"？靠的是 **State (状态)**。状态一变，界面自动刷新。

**项目实战**：
在 `App.jsx` 里找到这一行：
```javascript
const [analysisResult, setAnalysisResult] = useState(null);
```
*   `analysisResult`: 存数据的变量（一开始是空的）。
*   `setAnalysisResult`: 修改数据的**唯一**开关。

**逻辑流**：
1.  一开始 `analysisResult` 是 `null`。
2.  界面判断 `if (!analysisResult) return null;` -> 不显示分析结果。
3.  当 AI 分析完成后，代码调用 `setAnalysisResult("这首歌很悲伤...")`。
4.  React 发现数据变了，**自动**重新渲染界面 -> 显示出分析结果文字。

---

## 3. 核心概念：副作用 (Effect) - `useEffect`

**理论**：
有些事不是点击触发的，而是"当某件事发生后自动做的"。比如：只要分析结果出来了，就自动开始生成图片。

**项目实战**：
看看 `App.jsx` 里的这段代码：
```javascript
useEffect(() => {
  if (analysisResult) {
    // 只要 analysisResult 变成了有值状态
    // 就自动调用 generateImage() 去生成图片
    generateImage(analysisResult);
  }
}, [analysisResult]); // [analysisResult] 叫依赖项，意思是"盯着它，它变我就动"
```

---

## 4. 核心概念：通信 (Axios)

**理论**：
前端自己没有数据，数据都在后端。前端需要像打电话一样向后端要数据。我们用 `axios` 这个库来打电话。

**项目实战**：
在 `src/sections/Upload.jsx` (或者 `App.jsx` 的上传逻辑中)：
```javascript
const handleUpload = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  // 向后端发 POST 请求，地址是 /api/upload
  const response = await axios.post('http://localhost:8080/api/upload', formData);
  
  // 拿到后端回话: "上传成功，文件ID是 xxx"
  console.log(response.data); 
}
```

---

## 🎯 课后作业 (前端篇)

1.  **修改提示语**：找到上传组件，把 "点击上传音乐" 改成 "请投喂音乐"。
2.  **隐藏功能**：试着加一个状态 `isVisible`，控制某个组件的显示和隐藏（用 `isVisible && <Component />` 写法）。
3.  **理解流程**：用 `console.log` 跟踪一下，从点击上传到图片显示，数据是怎么在组件之间流动的？(提示：看 `App.jsx` 里传给子组件的 `props`)。
