import React from 'react'
import MusicPlayer from './MusicPlayer'
import './ResultDisplay.css'

export default function ResultDisplay({ result }) {
  if (!result) {
    return null
  }

  const { musicInfo, analysis, albumCover, trackId } = result

  // 解析 JSON 分析结果
  let analysisData = {}
  try {
    if (typeof analysis === 'string') {
      analysisData = JSON.parse(analysis)
    } else {
      analysisData = analysis
    }
  } catch (e) {
    console.warn('分析结果不是有效的 JSON:', analysis)
    analysisData = { text: analysis }
  }

  return (
    <div className="result-container">
      <div className="result-header">
        <h2>✨ 分析结果</h2>
        <p className="result-id">记录 ID: {trackId}</p>
      </div>

      {/* 音乐信息卡片 */}
      <div className="card">
        <div className="card-header">🎵 音乐信息</div>
        <div className="music-info">
          <div className="info-item">
            <label>歌名</label>
            <value>{musicInfo?.title || '未知'}</value>
          </div>
          <div className="info-item">
            <label>歌手</label>
            <value>{musicInfo?.artist || '未知'}</value>
          </div>
          <div className="info-item">
            <label>专辑</label>
            <value>{musicInfo?.album || '未知'}</value>
          </div>
        </div>
      </div>

      {/* AI 分析卡片 */}
      <div className="card">
        <div className="card-header">🤖 AI 分析结果</div>
        <div className="analysis-content">
          {analysisData.theme && (
            <div className="analysis-item">
              <span className="label">主题:</span>
              <span className="value">{analysisData.theme}</span>
            </div>
          )}
          {analysisData.mood && (
            <div className="analysis-item">
              <span className="label">氛围:</span>
              <span className="value">{analysisData.mood}</span>
            </div>
          )}
          {analysisData.visual_style && (
            <div className="analysis-item">
              <span className="label">视觉风格:</span>
              <span className="value">{analysisData.visual_style}</span>
            </div>
          )}
          {analysisData.colors && Array.isArray(analysisData.colors) && (
            <div className="analysis-item">
              <span className="label">推荐颜色:</span>
              <div className="color-palette">
                {analysisData.colors.map((color, idx) => (
                  <div key={idx} className="color-item" title={color}>
                    <div className="color-box" style={{ backgroundColor: color }}></div>
                    <span>{color}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
          {analysisData.image_prompt_en && (
            <div className="analysis-item">
              <span className="label">AI 绘画提示:</span>
              <p className="prompt-text">{analysisData.image_prompt_en}</p>
            </div>
          )}
          {analysisData.text && (
            <div className="analysis-item">
              <span className="label">详细分析:</span>
              <p className="prompt-text">{analysisData.text}</p>
            </div>
          )}
        </div>
      </div>

      {/* 专辑封面卡片 */}
      {albumCover && (
        <div className="card">
          <div className="card-header">🎨 生成的专辑封面</div>
          <div className="cover-display">
            <img
              src={`data:image/png;base64,${albumCover}`}
              alt="生成的专辑封面"
              className="cover-image"
            />
          </div>
        </div>
      )}

      {/* 操作按钮 */}
      <div className="result-actions">
        <button className="button button-primary" onClick={() => window.location.reload()}>
          🔄 重新上传
        </button>
        <button className="button button-secondary" onClick={() => {
          const json = JSON.stringify({
            musicInfo,
            analysis: analysisData,
            trackId
          }, null, 2)
          const blob = new Blob([json], { type: 'application/json' })
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `analysis-${trackId}.json`
          a.click()
        }}>
          📥 导出分析结果
        </button>
      </div>
    </div>
  )
}
