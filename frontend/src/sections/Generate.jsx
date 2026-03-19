import { useEffect, useState } from 'react'
import api from '../lib/api'
import './Generate.css'

const THEME_OPTIONS = ['爱情', '励志', '伤感', '怀旧', '治愈', '热血', '抒情']
const MOOD_OPTIONS = ['温柔', '忧郁', '激情', '宁静', '愉快', '孤独']
const STYLE_OPTIONS = ['黑白摄影', '手绘插画', '赛博朋克', '极简主义', '复古', '3D 渲染']

const ensureArray = (value) => (Array.isArray(value) ? value : [])

const labelsFromItems = (items) =>
  ensureArray(items)
    .map((item) => {
      if (typeof item === 'string') return item
      if (item && typeof item === 'object') return item.label || item.name || item.value || ''
      return ''
    })
    .filter(Boolean)

const formatNumber = (value, fractionDigits = 2) => {
  if (value === null || value === undefined || value === '') return '-'
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return String(value)
  return numberValue.toFixed(fractionDigits)
}

const formatBpm = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return String(value)
  return `${numberValue.toFixed(2)} BPM`
}

const formatTrackKey = (track) => {
  if (!track?.key) return ''
  return `${track.key}${track.scale ? ` ${track.scale}` : ''}`
}

const getMusicContext = (analysis, musicData) => {
  const source = analysis?.track || musicData?.track || musicData || {}
  const audioFeatures = analysis?.audioFeatures || musicData?.audioFeatures || {}
  const semanticProfile = analysis?.semanticProfile || musicData?.semanticProfile || {}
  const rhythm = audioFeatures.rhythm || {}
  const tonal = audioFeatures.tonal || {}
  const energy = audioFeatures.energy || {}
  const danceability = audioFeatures.danceability || {}

  const moods = labelsFromItems(semanticProfile.mood)
  const genres = labelsFromItems(semanticProfile.genre)
  const tags = labelsFromItems(semanticProfile.tags)

  const bpm = rhythm.bpm ?? musicData?.bpm ?? source.bpm
  const key = formatTrackKey(tonal) || musicData?.key || ''
  const theme = analysis?.theme || moods[0] || genres[0] || source.title || '音乐意境'
  const mood = analysis?.mood || moods[0] || energy.level || '氛围'
  const style = analysis?.visual_style || genres[0] || 'cinematic editorial'

  return {
    title: source.title || musicData?.title || '未知',
    artist: source.artist || musicData?.artist || '未知',
    album: source.album || musicData?.album || '未知',
    coverUrl: source.cover_url || musicData?.cover_url || '',
    bpm,
    key,
    energyLevel: energy.level || '',
    danceability: danceability.score,
    moods,
    genres,
    tags,
    theme,
    mood,
    style,
    prompt: analysis?.image_prompt_en || '',
    provider: audioFeatures.analyzer?.provider || '',
  }
}

const buildPromptFromContext = (context) => {
  const parts = [
    context.title ? `Album cover for "${context.title}"` : 'Album cover',
    context.artist && context.artist !== '未知' ? `by ${context.artist}` : '',
    context.album && context.album !== '未知' ? `from album "${context.album}"` : '',
    context.bpm ? `tempo ${context.bpm} BPM` : '',
    context.key ? `tonality ${context.key}` : '',
    context.energyLevel ? `${context.energyLevel} energy` : '',
    context.moods[0] ? `${context.moods[0]} mood` : '',
    context.genres[0] ? `${context.genres[0]} aesthetic` : '',
    context.tags.slice(0, 2).join(', '),
  ].filter(Boolean)

  if (parts.length === 0) {
    return 'Album cover, cinematic lighting, square composition, high detail, professional editorial artwork'
  }

  return `Album cover, ${parts.join(', ')}, cinematic lighting, rich texture, square composition, high detail, professional editorial artwork`
}

export default function Generate({ analysis, musicData }) {
  const [image, setImage] = useState('data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22300%22%3E%3Crect fill=%22%23f0f0f0%22 width=%22300%22 height=%22300%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 font-family=%22Arial%22 font-size=%2224%22 fill=%22%23999%22 text-anchor=%22middle%22 dominant-baseline=%22middle%22%3E生成的封面%3C/text%3E%3C/svg%3E')
  const [loading, setLoading] = useState(false)
  const [prompt, setPrompt] = useState('')
  const [history, setHistory] = useState([])
  const [theme, setTheme] = useState('')
  const [mood, setMood] = useState('')
  const [style, setStyle] = useState('')
  const [autoSyncPrompt, setAutoSyncPrompt] = useState(true)

  const fetchHistory = async () => {
    try {
      const res = await api.get('/ai/history')
      if (res.data.code === 200) {
        setHistory(res.data.data)
      }
    } catch (e) {
      console.error('获取历史记录失败', e)
    }
  }

  useEffect(() => {
    fetchHistory()
  }, [])

  useEffect(() => {
    const context = getMusicContext(analysis, musicData)
    const nextTheme = analysis?.theme || context.theme || ''
    const nextMood = analysis?.mood || context.mood || ''
    const nextStyle = analysis?.visual_style || context.style || ''

    setTheme(nextTheme)
    setMood(nextMood)
    setStyle(nextStyle)
    setPrompt(analysis?.image_prompt_en || buildPromptFromContext(context))
    setAutoSyncPrompt(true)
  }, [analysis, musicData])

  const buildPromptFromFields = (currentTheme, currentMood, currentStyle) => {
    const parts = []
    if (currentTheme) parts.push(`${currentTheme}主题`)
    if (currentMood) parts.push(`${currentMood}氛围`)
    if (currentStyle) parts.push(`${currentStyle}风格`)
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

  const handleGenerate = async () => {
    if (!prompt) return
    setLoading(true)
    try {
      const res = await api.post('/ai/generate-image', {
        prompt,
        steps: 30,
        width: 512,
        height: 512,
      })
      if (res.data.code === 200) {
        const imgData = `data:image/png;base64,${res.data.data}`
        setImage(imgData)
        fetchHistory()
      } else {
        alert(res.data.message || '生成失败')
      }
    } catch (e) {
      alert('生成失败: ' + (e.response?.data?.message || e.message))
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

  const context = getMusicContext(analysis, musicData)
  const semanticTags = [...context.moods, ...context.genres, ...context.tags].filter(Boolean)

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
            <div className="context-card">
              <h3>分析依据</h3>
              <div className="context-grid">
                <div className="context-stat">
                  <span>歌曲</span>
                  <strong>{context.title}</strong>
                  <small>{context.artist}</small>
                </div>
                <div className="context-stat">
                  <span>节奏 / 调性</span>
                  <strong>{formatBpm(context.bpm)}</strong>
                  <small>{context.key || '-'}</small>
                </div>
                <div className="context-stat">
                  <span>能量 / 舞动感</span>
                  <strong>{context.energyLevel || '-'}</strong>
                  <small>{context.danceability != null ? `舞动感 ${formatNumber(context.danceability, 4)}` : '未提供'}</small>
                </div>
                <div className="context-stat">
                  <span>来源</span>
                  <strong>{analysis?.image_prompt_en ? 'AI 提示词' : '结构化特征'}</strong>
                  <small>{context.provider || 'librosa'}</small>
                </div>
              </div>

              <div className="context-tags">
                {semanticTags.length > 0 ? (
                  semanticTags.map((item) => (
                    <span key={item} className="context-tag">
                      {item}
                    </span>
                  ))
                ) : (
                  <span className="context-tag context-tag-muted">暂无语义标签</span>
                )}
              </div>

              <div className="context-note">
                {analysis?.image_prompt_en
                  ? '当前提示词来自 AI 分析，可继续手动微调。'
                  : '当前提示词由本地音频特征与语义标签自动拼接。'}
              </div>
            </div>

            <div className="info-section">
              <h3>生成信息</h3>
              <div className="info-detail">
                <label>主题</label>
                <div className="field-row">
                  <select value={THEME_OPTIONS.includes(theme) ? theme : ''} onChange={(e) => handleThemeChange(e.target.value)}>
                    <option value="">从列表选择</option>
                    {THEME_OPTIONS.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
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
                  <select value={MOOD_OPTIONS.includes(mood) ? mood : ''} onChange={(e) => handleMoodChange(e.target.value)}>
                    <option value="">从列表选择</option>
                    {MOOD_OPTIONS.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
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
                  <select value={STYLE_OPTIONS.includes(style) ? style : ''} onChange={(e) => handleStyleChange(e.target.value)}>
                    <option value="">从列表选择</option>
                    {STYLE_OPTIONS.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
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
              <button className="btn btn-primary" onClick={handleGenerate} disabled={loading || !prompt}>
                {loading ? '生成中...' : '开始生成'}
              </button>
              <button className="btn btn-secondary" onClick={handleDownload} disabled={image.includes('svg')}>
                下载
              </button>
            </div>
          </div>
        </div>

        {history.length > 0 && (
          <div className="history-section">
            <h3>历史生成记录</h3>
            <div className="history-grid">
              {history.map((item) => (
                <div
                  key={item.id}
                  className="history-item"
                  onClick={() => {
                    if (item.coverImageBase64) {
                      setImage(`data:image/png;base64,${item.coverImageBase64}`)
                    }
                    setPrompt(item.promptSummary || '')
                    setAutoSyncPrompt(false)
                  }}
                  title={item.promptSummary}
                >
                  {item.coverImageBase64 ? (
                    <img src={`data:image/png;base64,${item.coverImageBase64}`} alt="history" />
                  ) : (
                    <div style={{ background: '#eee', width: '100%', height: '100%' }}></div>
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
