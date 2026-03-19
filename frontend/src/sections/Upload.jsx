import { useState } from 'react'
import api from '../lib/api'
import './Upload.css'

export default function Upload({ onUploadSuccess, neteaseData, onClearNetease, onAudioSelected }) {
  const [file, setFile] = useState(null)
  const [isDragging, setIsDragging] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('')

  const handleDrop = (e) => {
    e.preventDefault()
    setIsDragging(false)
    const droppedFile = e.dataTransfer.files[0]
    if (droppedFile?.type.startsWith('audio/')) {
      setFile(droppedFile)
      setMessage('')
      if (onAudioSelected) {
        const url = URL.createObjectURL(droppedFile)
        onAudioSelected(url)
      }
    } else {
      setMessage('请选择音频文件')
      setMessageType('error')
    }
  }

  const handleUpload = async () => {
    if (!file) return

    try {
      setIsLoading(true)
      setProgress(0)
      const formData = new FormData()
      formData.append('file', file)

      // 第一步：仅上传文件，获取 uploadId
      const uploadRes = await api.post(
        '/upload/audio',
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (e) => setProgress(Math.round((e.loaded / e.total) * 100))
        }
      )

      if (uploadRes.data.code !== 200) {
        setMessage(uploadRes.data.message || '上传失败')
        setMessageType('error')
        return
      }

      const uploadId = uploadRes.data.data.uploadId

      // 第二步：调用后端音乐识别接口，使用真实音频信息
      let musicInfo = null
      let featureInfo = null
      let semanticInfo = null
      try {
        const recognizeRes = await api.post(
          '/music/recognize',
          new URLSearchParams({ uploadId, useSeparatedVocals: 'true' })
        )
        if (recognizeRes.data.code === 200) {
          musicInfo = recognizeRes.data.data
        }
      } catch (e) {
        // 识别失败时不阻断整体流程，只提示
        console.error('音乐识别失败', e)
      }

      try {
        const [featuresRes, semanticRes] = await Promise.allSettled([
          api.post('/music/features', new URLSearchParams({ uploadId, includeStems: 'true' })),
          api.post('/music/semantic', new URLSearchParams({ uploadId }))
        ])

        if (featuresRes.status === 'fulfilled' && featuresRes.value.data.code === 200) {
          featureInfo = featuresRes.value.data.data
        } else if (featuresRes.status === 'rejected') {
          console.error('音频特征分析失败', featuresRes.reason)
        }

        if (semanticRes.status === 'fulfilled' && semanticRes.value.data.code === 200) {
          semanticInfo = semanticRes.value.data.data
        } else if (semanticRes.status === 'rejected') {
          console.error('音乐语义分析失败', semanticRes.reason)
        }
      } catch (e) {
        console.error('音频增强分析失败', e)
      }

      setMessage('上传成功')
      setMessageType('success')

      // 向上抛出用于分析页的真实音乐信息
      if (onUploadSuccess) {
        const normalizedTitle = musicInfo?.title?.trim() || file.name.replace(/\.[^.]+$/, '')
        const normalizedArtist = musicInfo?.artist?.trim() || '未知'
        const normalizedAlbum = musicInfo?.album?.trim() || '未知'
        const track = {
          title: normalizedTitle,
          artist: normalizedArtist,
          album: normalizedAlbum,
          cover_url: musicInfo?.cover_url || '',
          lyrics: musicInfo?.lyrics || '',
          type: musicInfo?.type || 'audio',
          uploadId,
        }

        const bpm = featureInfo?.rhythm?.bpm
        const tonalKey = featureInfo?.tonal?.key
        const tonalScale = featureInfo?.tonal?.scale
        onUploadSuccess({
          ...(musicInfo || {}),
          ...track,
          uploadId,
          bpm,
          key: tonalKey ? `${tonalKey} ${tonalScale || ''}`.trim() : undefined,
          audioFeatures: featureInfo,
          semanticProfile: semanticInfo,
          track,
          analysisSnapshot: {
            rhythm: featureInfo?.rhythm || null,
            tonal: featureInfo?.tonal || null,
            energy: featureInfo?.energy || null,
            spectral: featureInfo?.spectral || null,
            danceability: featureInfo?.danceability || null,
            chords: featureInfo?.chords || null,
          },
        })
      }
    } catch (error) {
      const msg = error.response?.data?.message || error.message || '上传失败'
      setMessage(`上传失败: ${msg}`)
      setMessageType('error')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="upload">
      <div className="container">
        <h2>上传音乐</h2>

        {/* 网易云解析结果展示模块 */}
        {neteaseData && (
          <div className="netease-result-card">
            <div className="netease-result-header">
              <h3>网易云链接解析结果</h3>
              <button className="btn-close" onClick={onClearNetease}>×</button>
            </div>
            <div className="netease-result-content">
              <div className="netease-cover">
                {neteaseData.cover_url ? (
                  <img src={neteaseData.cover_url} alt={neteaseData.title} />
                ) : (
                  <div className="cover-placeholder">♫</div>
                )}
              </div>
              <div className="netease-info">
                <div className="info-item">
                  <label>音乐标题</label>
                  <p>{neteaseData.title}</p>
                </div>
                <div className="info-item">
                  <label>艺术家</label>
                  <p>{neteaseData.artist}</p>
                </div>
                <div className="info-item">
                  <label>专辑</label>
                  <p>{neteaseData.album}</p>
                </div>
                <div className="info-item">
                  <label>类型</label>
                  <p>{neteaseData.type}</p>
                </div>
              </div>
            </div>
          </div>
        )}
        
        <div className="upload-box">
          {/* 网易云解析数据时显示已选中的信息提示 */}
          {neteaseData && (
            <div className="netease-selected-hint">
              已从网易云链接导入：<strong>{neteaseData.title}</strong> - <strong>{neteaseData.artist}</strong>
            </div>
          )}
          <div
            className={`upload-area ${isDragging ? 'dragging' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setIsDragging(true) }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            onClick={() => document.getElementById('file-input').click()}
          >
            <div className="upload-icon">♫</div>
            <p>拖拽音乐文件到此，或点击选择</p>
            <input
              id="file-input"
              type="file"
              accept="audio/*"
              style={{ display: 'none' }}
              onChange={(e) => {
                const selectedFile = e.target.files?.[0]
                if (selectedFile) {
                  setFile(selectedFile)
                  if (onAudioSelected) {
                    const url = URL.createObjectURL(selectedFile)
                    onAudioSelected(url)
                  }
                }
              }}
            />
          </div>

          {file && (
            <div className="file-info">
              <span>{file.name}</span>
              <button
                className="btn-remove"
                onClick={() => {
                  setFile(null)
                  if (onAudioSelected) {
                    onAudioSelected(null)
                  }
                }}
              >
                ×
              </button>
            </div>
          )}

          {isLoading && (
            <div className="progress">
              <div className="progress-bar" style={{ width: `${progress}%` }}></div>
              <span>{progress}%</span>
            </div>
          )}

          {message && (
            <div className={`message ${messageType}`}>
              {message}
            </div>
          )}

          <button
            className="btn btn-primary"
            onClick={handleUpload}
            disabled={!file || isLoading}
            style={{ width: '100%', marginTop: '20px' }}
          >
            {isLoading ? '处理中...' : '开始处理'}
          </button>
        </div>
      </div>
    </div>
  )
}
