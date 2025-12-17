import { useState } from 'react'
import './Generate.css'

export default function Generate({ aiAnalysis, onNavigate }) {
  const [generatedImage, setGeneratedImage] = useState('data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22300%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22300%22 height=%22300%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 font-family=%22Arial%22 font-size=%2224%22 fill=%22%239ca3af%22 text-anchor=%22middle%22 dominant-baseline=%22middle%22%3E图片生成中...%3C/text%3E%3C/svg%3E')
  const [isGenerating, setIsGenerating] = useState(false)

  const handleRegenerate = async () => {
    setIsGenerating(true)
    // TODO: 调用后端API生成图片
    setTimeout(() => {
      setIsGenerating(false)
    }, 2000)
  }

  const handleDownload = () => {
    const link = document.createElement('a')
    link.href = generatedImage
    link.download = 'album-cover.png'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="generate">
      <h1>专辑封面生成</h1>
      
      <div className="generate-layout">
        {/* 左侧：生成的图片 */}
        <div className="generate-image">
          <div className="image-container">
            <img src={generatedImage} alt="生成的专辑封面" />
            {isGenerating && <div className="generating-overlay">生成中...</div>}
          </div>
          
          <div className="image-actions">
            <button className="btn btn-primary" onClick={handleRegenerate} disabled={isGenerating}>
              {isGenerating ? '生成中...' : '重新生成'}
            </button>
            <button className="btn btn-primary" onClick={handleDownload}>
              下载图片
            </button>
          </div>
        </div>

        {/* 右侧：AI分析信息 */}
        <div className="generate-info">
          <div className="card">
            <div className="card-header">AI 生成建议</div>
            
            <div className="info-section">
              <h4>主题</h4>
              <p>{aiAnalysis?.theme || '-'}</p>
            </div>

            <div className="info-section">
              <h4>氛围</h4>
              <p>{aiAnalysis?.mood || '-'}</p>
            </div>

            <div className="info-section">
              <h4>视觉风格</h4>
              <p>{aiAnalysis?.visual_style || '-'}</p>
            </div>

            <div className="info-section">
              <h4>使用的 Prompt</h4>
              <p className="prompt-text">{aiAnalysis?.image_prompt_en || '-'}</p>
            </div>

            <div className="info-section">
              <h4>配色方案</h4>
              <div className="color-palette">
                {aiAnalysis?.colors?.map((color, idx) => (
                  <div key={idx} className="color-item" style={{ backgroundColor: color }} title={color}></div>
                ))}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">操作</div>
            <button className="btn btn-primary" onClick={() => onNavigate('upload')} style={{ width: '100%', marginBottom: '8px' }}>
              生成新专辑
            </button>
            <button className="btn btn-secondary" onClick={() => onNavigate('home')} style={{ width: '100%' }}>
              返回首页
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
