import { useEffect, useState } from 'react'
import api from '../lib/api'
import './Analysis.css'

const DEFAULT_AI_ANALYSIS = {
  theme: '未知',
  mood: '未知',
  visual_style: '未知',
  colors: ['#D93C39', '#FF6565', '#FFB3B0'],
  image_prompt_en: 'Generate a professional album cover',
}

const formatNumber = (value, fractionDigits = 2) => {
  if (value === null || value === undefined || value === '') return '-'
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return String(value)
  return numberValue.toFixed(fractionDigits)
}

const formatPercent = (value, fractionDigits = 1) => {
  if (value === null || value === undefined || value === '') return '-'
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return String(value)
  return `${(numberValue * 100).toFixed(fractionDigits)}%`
}

const ensureArray = (value) => (Array.isArray(value) ? value : [])

const labelsFromItems = (items) =>
  ensureArray(items)
    .map((item) => {
      if (typeof item === 'string') return item
      if (item && typeof item === 'object') return item.label || item.name || item.value || ''
      return ''
    })
    .filter(Boolean)

const formatList = (value, fallback = '-') => {
  const labels = labelsFromItems(value)
  return labels.length > 0 ? labels.join(' / ') : fallback
}

const formatKey = (tonal) => {
  if (!tonal) return '-'
  if (!tonal.key) return '-'
  return `${tonal.key}${tonal.scale ? ` ${tonal.scale}` : ''}`
}

const formatHistogram = (items) => {
  const labels = ensureArray(items)
    .slice(0, 3)
    .map((item) => {
      const label = item?.label || 'Unknown'
      const ratio = formatPercent(item?.ratio, 1)
      return `${label} ${ratio}`
    })
  return labels.length > 0 ? labels.join(' / ') : '-'
}

const getTrackInfo = (musicData) => {
  const track = musicData?.track || {}
  return {
    title: track.title || musicData?.title || '-',
    artist: track.artist || musicData?.artist || '-',
    album: track.album || musicData?.album || '-',
    coverUrl: track.cover_url || musicData?.cover_url || '',
    uploadId: track.uploadId ?? musicData?.uploadId ?? '-',
    type: track.type || musicData?.type || 'audio',
  }
}

export default function Analysis({ musicData, onAnalysisComplete }) {
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!musicData) {
      setAnalysis(null)
      setLoading(false)
      return
    }

    let cancelled = false

    const fetchAnalysis = async () => {
      try {
        setLoading(true)
        const response = await api.post('/ai/analyze', {
          title: musicData.title,
          artist: musicData.artist,
          album: musicData.album,
          lyrics: musicData.lyrics || '',
          audioProfile: musicData.audioFeatures || null,
          semanticProfile: musicData.semanticProfile || null,
        })

        if (cancelled) return

        if (response.data.code === 200) {
          setAnalysis(response.data.data || DEFAULT_AI_ANALYSIS)
          setError('')
        } else {
          setAnalysis(DEFAULT_AI_ANALYSIS)
          setError(response.data.message || '分析失败')
        }
      } catch (err) {
        if (cancelled) return
        setAnalysis(DEFAULT_AI_ANALYSIS)
        setError(err.response?.data?.message || 'API 请求失败，使用默认分析')
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    fetchAnalysis()

    return () => {
      cancelled = true
    }
  }, [musicData])

  const handleGenerate = () => {
    if (!analysis) return

    onAnalysisComplete({
      ...analysis,
      track: getTrackInfo(musicData),
      audioFeatures: musicData?.audioFeatures || null,
      semanticProfile: musicData?.semanticProfile || null,
    })
  }

  const trackInfo = getTrackInfo(musicData)
  const audioFeatures = musicData?.audioFeatures || {}
  const semanticProfile = musicData?.semanticProfile || {}
  const rhythm = audioFeatures.rhythm || {}
  const tonal = audioFeatures.tonal || {}
  const energy = audioFeatures.energy || {}
  const spectral = audioFeatures.spectral || {}
  const danceability = audioFeatures.danceability || {}
  const chords = audioFeatures.chords || {}
  const stems = audioFeatures.stems || {}
  const embedding = semanticProfile.embedding || {}

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
              {trackInfo.coverUrl && (
                <div className="cover-preview" style={{ width: '100px', height: '100px', flexShrink: 0 }}>
                  <img
                    src={trackInfo.coverUrl}
                    alt="Album Cover"
                    style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '4px' }}
                  />
                </div>
              )}

              <div className="info-list" style={{ flex: 1 }}>
                <div className="info-item">
                  <label>歌名</label>
                  <span>{trackInfo.title}</span>
                </div>
                <div className="info-item">
                  <label>歌手</label>
                  <span>{trackInfo.artist}</span>
                </div>
                <div className="info-item">
                  <label>专辑</label>
                  <span>{trackInfo.album}</span>
                </div>
                <div className="info-item">
                  <label>上传 ID</label>
                  <span>{trackInfo.uploadId}</span>
                </div>
                <div className="info-item">
                  <label>分析引擎</label>
                  <span>{audioFeatures?.analyzer?.provider || 'librosa'}</span>
                </div>
                <div className="info-item">
                  <label>特征版本</label>
                  <span>{audioFeatures?.analyzer?.features_version || 'v2'}</span>
                </div>
                <div className="info-item">
                  <label>来源类型</label>
                  <span>{trackInfo.type}</span>
                </div>
              </div>
            </div>

            <div className="analysis-summary-grid">
              <div className="metric-card">
                <span>BPM</span>
                <strong>{formatNumber(rhythm.bpm)}</strong>
                <small>{rhythm.beat_count ? `${rhythm.beat_count} 个拍点` : '未检测到拍点'}</small>
              </div>
              <div className="metric-card">
                <span>调性</span>
                <strong>{formatKey(tonal)}</strong>
                <small>{tonal.confidence != null ? `置信度 ${formatPercent(tonal.confidence, 1)}` : '未提供置信度'}</small>
              </div>
              <div className="metric-card">
                <span>能量</span>
                <strong>{energy.level || '-'}</strong>
                <small>{energy.rms_mean != null ? `RMS ${formatNumber(energy.rms_mean, 5)}` : '未提供能量特征'}</small>
              </div>
              <div className="metric-card">
                <span>舞动感</span>
                <strong>{danceability.score != null ? formatNumber(danceability.score, 4) : '-'}</strong>
                <small>{danceability.confidence != null ? `置信度 ${formatPercent(danceability.confidence, 0)}` : '未提供置信度'}</small>
              </div>
            </div>
          </div>

          <div className="analysis-card">
            <h3>AI 画面建议</h3>
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
                {(analysis?.colors || DEFAULT_AI_ANALYSIS.colors).map((color, idx) => (
                  <div key={`${color}-${idx}`} className="color" style={{ backgroundColor: color }} title={color}></div>
                ))}
              </div>
            </div>

            <div className="prompt-box" style={{ marginTop: '16px' }}>
              <label>生成 Prompt</label>
              <p>{analysis?.image_prompt_en || DEFAULT_AI_ANALYSIS.image_prompt_en}</p>
            </div>
          </div>
        </div>

        <div className="analysis-card" style={{ marginTop: '24px' }}>
          <h3>音频特征</h3>
          <div className="feature-grid">
            <div className="feature-box">
              <label>节奏</label>
              <p>
                BPM {formatNumber(rhythm.bpm)}
                <br />
                拍点 {rhythm.beat_count ?? '-'}
                <br />
                稳定性 {rhythm.tempo_stable ? '稳定' : '待确认'}
              </p>
            </div>
            <div className="feature-box">
              <label>调性画像</label>
              <p>
                {formatKey(tonal)}
                <br />
                关键音高 {formatList(tonal.dominant_pitches)}
              </p>
            </div>
            <div className="feature-box">
              <label>能量</label>
              <p>
                等级 {energy.level || '-'}
                <br />
                RMS {energy.rms_mean != null ? formatNumber(energy.rms_mean, 5) : '-'}
                <br />
                波动 {energy.rms_std != null ? formatNumber(energy.rms_std, 5) : '-'}
              </p>
            </div>
            <div className="feature-box">
              <label>频谱</label>
              <p>
                质心 {spectral.centroid_mean != null ? formatNumber(spectral.centroid_mean, 2) : '-'}
                <br />
                带宽 {spectral.bandwidth_mean != null ? formatNumber(spectral.bandwidth_mean, 2) : '-'}
                <br />
                rolloff {spectral.rolloff_mean != null ? formatNumber(spectral.rolloff_mean, 2) : '-'}
              </p>
            </div>
            <div className="feature-box">
              <label>舞动感</label>
              <p>
                分数 {danceability.score != null ? formatNumber(danceability.score, 4) : '-'}
                <br />
                置信度 {danceability.confidence != null ? formatPercent(danceability.confidence, 0) : '-'}
              </p>
            </div>
            <div className="feature-box">
              <label>和弦</label>
              <p>
                变化率 {chords.changes_per_minute != null ? formatNumber(chords.changes_per_minute, 2) : '-'} / min
                <br />
                重点分布 {formatHistogram(chords.histogram)}
              </p>
            </div>
          </div>
        </div>

        {musicData?.semanticProfile && (
          <div className="analysis-card" style={{ marginTop: '24px' }}>
            <h3>音乐语义理解</h3>
            <div className="feature-grid">
              <div className="feature-box">
                <label>情绪标签</label>
                <p>{formatList(semanticProfile.mood)}</p>
              </div>
              <div className="feature-box">
                <label>风格标签</label>
                <p>{formatList(semanticProfile.genre)}</p>
              </div>
              <div className="feature-box">
                <label>语义标签</label>
                <p>{formatList(semanticProfile.tags)}</p>
              </div>
              <div className="feature-box">
                <label>Embedding</label>
                <p>
                  {embedding.available === false ? '未启用' : embedding.available === true ? '已启用' : '未提供'}
                  <br />
                  模型 {embedding.model || semanticProfile.model || '-'}
                  <br />
                  维度 {embedding.hidden_size || '-'}
                </p>
              </div>
              <div className="feature-box">
                <label>推理信息</label>
                <p>
                  设备 {embedding.device || semanticProfile.device || '-'}
                  <br />
                  窗口 {embedding.window_count || '-'}
                  <br />
                  范数 {embedding.norm != null ? formatNumber(embedding.norm, 4) : '-'}
                </p>
              </div>
            </div>

            <div className="tag-list" style={{ marginTop: '12px' }}>
              {labelsFromItems(semanticProfile.mood).map((item) => (
                <span key={`mood-${item}`} className="tag">
                  {item}
                </span>
              ))}
              {labelsFromItems(semanticProfile.genre).map((item) => (
                <span key={`genre-${item}`} className="tag">
                  {item}
                </span>
              ))}
              {labelsFromItems(semanticProfile.tags).map((item) => (
                <span key={`tag-${item}`} className="tag">
                  {item}
                </span>
              ))}
            </div>
          </div>
        )}

        {Object.keys(stems).length > 0 && (
          <div className="analysis-card" style={{ marginTop: '24px' }}>
            <h3>Demucs 分轨摘要</h3>
            <div className="feature-grid">
              {Object.entries(stems).map(([stemName, stemInfo]) => (
                <div key={stemName} className="feature-box">
                  <label>{stemName}</label>
                  <p>
                    BPM {stemInfo?.rhythm?.bpm != null ? formatNumber(stemInfo.rhythm.bpm) : '-'}
                    <br />
                    调性 {formatKey(stemInfo?.tonal)}
                    <br />
                    能量 {stemInfo?.energy?.level || '-'}
                  </p>
                </div>
              ))}
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
