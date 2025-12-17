import { useState, useEffect } from 'react'
import axios from 'axios'
import './Analysis.css'

const API_BASE = 'http://localhost:8081/api'

export default function Analysis({ musicData, onAnalysisComplete }) {
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!musicData) return

    const fetchAnalysis = async () => {
      try {
        setLoading(true)
        // 调用后端AI解析API
        const response = await axios.post(`${API_BASE}/ai/analyze`, {
          title: musicData.title,
          artist: musicData.artist,
          album: musicData.album,
          lyrics: musicData.lyrics || ''
        })

        if (response.data.code === 200) {
          setAnalysis(response.data.data)
          setError('')
        } else {
          setError('分析失败')
        }
      } catch (err) {
        // 分析失败时使用默认模拟数据
        setAnalysis({
          theme: '未知',
          mood: '未知',
          visual_style: '未知',
          colors: ['#D93C39', '#FF6565', '#FFB3B0']
        })
        setError('API请求失败，使用默认分析')
      } finally {
        setLoading(false)
      }
    }

    fetchAnalysis()
  }, [musicData])

  const handleGenerate = () => {
    if (analysis) {
      onAnalysisComplete(analysis)
    }
  }

  if (loading) {
    return (
      <div className="analysis">
        <div className="container">
          <h2>AI 分析结果</h2>
          <div className="loading-state">正在分析音乐特征...</div>
        </div>
      </div>
    )
  }

  return (
    <div className="analysis">
      <div className="container">
        <h2>AI 分析结果</h2>
        
        {error && <div className="message error">{error}</div>}
        
        <div className="analysis-grid">
          <div className="analysis-card music-info">
            <h3>音乐信息</h3>
            <div className="info-content" style={{ display: 'flex', gap: '20px' }}>
              {musicData?.cover_url && (
                <div className="cover-preview" style={{ width: '100px', height: '100px', flexShrink: 0 }}>
                  <img 
                    src={musicData.cover_url} 
                    alt="Album Cover" 
                    style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '4px' }} 
                  />
                </div>
              )}
              <div className="info-list" style={{ flex: 1 }}>
                <div className="info-item">
                  <label>歌名</label>
                  <span>{musicData?.title || '-'}</span>
                </div>
                <div className="info-item">
                  <label>歌手</label>
                  <span>{musicData?.artist || '-'}</span>
                </div>
                <div className="info-item">
                  <label>专辑</label>
                  <span>{musicData?.album || '-'}</span>
                </div>
              {musicData?.bpm && (
                <div className="info-item">
                  <label>BPM</label>
                  <span>{musicData.bpm}</span>
                </div>
              )}
              {musicData?.key && (
                <div className="info-item">
                  <label>调性</label>
                  <span>{musicData.key}</span>
                </div>
              )}
            </div>
            </div>
          </div>

          <div className="analysis-card">
            <h3>特征识别</h3>
            <div className="feature-grid">
              <div className="feature-box">
                <label>主题</label>
                <p>{analysis?.theme || '分析中...'}</p>
              </div>
              <div className="feature-box">
                <label>氛围</label>
                <p>{analysis?.mood || '分析中...'}</p>
              </div>
              <div className="feature-box">
                <label>风格</label>
                <p>{analysis?.visual_style || '分析中...'}</p>
              </div>
            </div>

            <div className="color-palette">
              <label>配色</label>
              <div className="colors">
                {analysis?.colors && analysis.colors.map((color, idx) => (
                  <div key={idx} className="color" style={{ backgroundColor: color }} title={color}></div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {analysis?.prompt && (
          <div className="analysis-card" style={{ marginTop: '24px' }}>
            <h3>生成 Prompt</h3>
            <div className="prompt-box">
              <p>{analysis.prompt}</p>
            </div>
          </div>
        )}

        <button 
          className="btn btn-primary" 
          onClick={handleGenerate} 
          style={{ width: '100%', marginTop: '40px', padding: '12px' }}
          disabled={!analysis}
        >
          生成封面 →
        </button>
      </div>
    </div>
  )
}
