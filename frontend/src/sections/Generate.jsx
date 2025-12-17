import { useState, useEffect } from 'react'
import axios from 'axios'
import './Generate.css'

const API_BASE = 'http://localhost:8081/api'

const THEME_OPTIONS = ['爱情', '励志', '伤感', '怀旧', '治愈', '热血', '抒情']
const MOOD_OPTIONS = ['温柔', '忧郁', '激情', '宁静', '愉快', '孤独']
const STYLE_OPTIONS = ['黑白摄影', '手绘插画', '赛博朋克', '极简主义', '复古', '3D 渲染']

export default function Generate({ analysis, onImageGenerated }) {
  const [image, setImage] = useState('data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22300%22%3E%3Crect fill=%22%23f0f0f0%22 width=%22300%22 height=%22300%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 font-family=%22Arial%22 font-size=%2224%22 fill=%22%23999%22 text-anchor=%22middle%22 dominant-baseline=%22middle%22%3E生成的封面%3C/text%3E%3C/svg%3E')
  const [loading, setLoading] = useState(false)
  const [prompt, setPrompt] = useState('')
  const [history, setHistory] = useState([])
  const [theme, setTheme] = useState('')
  const [mood, setMood] = useState('')
  const [style, setStyle] = useState('')
  const [autoSyncPrompt, setAutoSyncPrompt] = useState(true)

  useEffect(() => {
    if (analysis) {
      const p = analysis.image_prompt_en || `Album cover, ${analysis.theme} theme, ${analysis.mood} mood, ${analysis.visual_style} style, high quality`
      setPrompt(p)
      setTheme(analysis.theme || '')
      setMood(analysis.mood || '')
      setStyle(analysis.visual_style || '')
      setAutoSyncPrompt(true)
    }
    fetchHistory()
  }, [analysis])

  const buildPromptFromFields = (t, m, s) => {
    const parts = []
    if (t) parts.push(`${t}主题`)
    if (m) parts.push(`${m}氛围`)
    if (s) parts.push(`${s}风格`)
    if (parts.length === 0) return ''
    return `Album cover, ${parts.join(', ')}, high quality, detailed`
  }

  const handleThemeChange = (value) => {
    setTheme(value)
    if (autoSyncPrompt) {
      setPrompt(buildPromptFromFields(value, mood, style))
    }
  }

  const handleMoodChange = (value) => {
    setMood(value)
    if (autoSyncPrompt) {
      setPrompt(buildPromptFromFields(theme, value, style))
    }
  }

  const handleStyleChange = (value) => {
    setStyle(value)
    if (autoSyncPrompt) {
      setPrompt(buildPromptFromFields(theme, mood, value))
    }
  }

  const fetchHistory = async () => {
    try {
      const res = await axios.get(`${API_BASE}/ai/history`)
      if (res.data.code === 200) {
        setHistory(res.data.data)
      }
    } catch (e) {
      console.error("获取历史记录失败", e)
    }
  }

  const handleGenerate = async () => {
    if (!prompt) return
    setLoading(true)
    try {
      const res = await axios.post(`${API_BASE}/ai/generate-image`, {
        prompt: prompt,
        steps: 30,
        width: 512,
        height: 512
      })
      if (res.data.code === 200) {
        const imgData = `data:image/png;base64,${res.data.data}`
        setImage(imgData)
        if (onImageGenerated) onImageGenerated(imgData)
        fetchHistory()
      } else {
        alert(res.data.message || '生成失败')
      }
    } catch (e) {
      alert("生成失败: " + (e.response?.data?.message || e.message))
    } finally {
      setLoading(false)
    }
  }

  const handleDownload = () => {
    const link = document.createElement('a')
    link.href = image
    link.download = `album-cover-${Date.now()}.png`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="generate">
      <div className="container">
        <h2>专辑封面生成</h2>
        
        <div className="generate-grid">
          <div className="image-box">
            <img src={image} alt="生成的专辑封面" />
            {loading && (
              <div className="loading-overlay">
                <div className="spinner"></div>
                <div>正在绘制中...</div>
              </div>
            )}
          </div>

          <div className="info-box">
            <div className="info-section">
              <h3>生成信息</h3>
              <div className="info-detail">
                <label>主题</label>
                <div className="field-row">
                  <select 
                    value={THEME_OPTIONS.includes(theme) ? theme : ''}
                    onChange={(e) => handleThemeChange(e.target.value)}
                  >
                    <option value="">从列表选择</option>
                    {THEME_OPTIONS.map((item) => (
                      <option key={item} value={item}>{item}</option>
                    ))}
                  </select>
                  <input
                    type="text"
                    value={theme}
                    onChange={(e) => handleThemeChange(e.target.value)}
                    placeholder="或自定义输入主题"
                  />
                </div>
              </div>
              <div className="info-detail">
                <label>氛围</label>
                <div className="field-row">
                  <select 
                    value={MOOD_OPTIONS.includes(mood) ? mood : ''}
                    onChange={(e) => handleMoodChange(e.target.value)}
                  >
                    <option value="">从列表选择</option>
                    {MOOD_OPTIONS.map((item) => (
                      <option key={item} value={item}>{item}</option>
                    ))}
                  </select>
                  <input
                    type="text"
                    value={mood}
                    onChange={(e) => handleMoodChange(e.target.value)}
                    placeholder="或自定义输入氛围"
                  />
                </div>
              </div>
              <div className="info-detail">
                <label>风格</label>
                <div className="field-row">
                  <select 
                    value={STYLE_OPTIONS.includes(style) ? style : ''}
                    onChange={(e) => handleStyleChange(e.target.value)}
                  >
                    <option value="">从列表选择</option>
                    {STYLE_OPTIONS.map((item) => (
                      <option key={item} value={item}>{item}</option>
                    ))}
                  </select>
                  <input
                    type="text"
                    value={style}
                    onChange={(e) => handleStyleChange(e.target.value)}
                    placeholder="或自定义输入风格"
                  />
                </div>
              </div>
              <div className="info-detail">
                <label>提示词 (Prompt)</label>
                <textarea 
                  value={prompt}
                  onChange={(e) => {
                    setPrompt(e.target.value)
                    setAutoSyncPrompt(false)
                  }}
                  placeholder="输入提示词..."
                />
              </div>
            </div>

            <div className="actions">
              <button 
                className="btn btn-primary" 
                onClick={handleGenerate}
                disabled={loading}
              >
                {loading ? '生成中...' : '开始生成'}
              </button>
              <button 
                className="btn btn-secondary" 
                onClick={handleDownload}
                disabled={image.includes('svg')}
              >
                下载
              </button>
            </div>
          </div>
        </div>

        {/* 历史记录 */}
        {history.length > 0 && (
          <div className="history-section">
            <h3>历史生成记录</h3>
            <div className="history-grid">
              {history.map((item) => (
                <div 
                  key={item.id} 
                  className="history-item"
                  onClick={() => {
                    setImage(`data:image/png;base64,${item.albumCover}`)
                    if (item.aiAnalysis && item.aiAnalysis.startsWith("Prompt: ")) {
                        setPrompt(item.aiAnalysis.substring(8))
                    }
                  }}
                  title={item.aiAnalysis}
                >
                  {item.albumCover ? (
                     <img src={`data:image/png;base64,${item.albumCover}`} alt="history" />
                  ) : (
                    <div style={{background: '#eee', width: '100%', height: '100%'}}></div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
