import { useState } from 'react'
import axios from 'axios'
import './Hero.css'

const API_BASE = 'http://localhost:8081/api'

export default function Hero({ onNavigate, uploadRef, onNeteaseImportSuccess }) {
  const [activeTab, setActiveTab] = useState('upload')
  const [neteaseUrl, setNeteaseUrl] = useState('')
  const [neteaseLoading, setNeteaseLoading] = useState(false)
  const [neteaseError, setNeteaseError] = useState('')

  const handleNeteaseImport = async () => {
    if (!neteaseUrl.trim()) {
      setNeteaseError('请输入网易云链接')
      return
    }

    try {
      setNeteaseLoading(true)
      setNeteaseError('')

      console.log('[网易云解析] 开始请求...', neteaseUrl)
      
      // 调用后端网易云解析API
      const response = await axios.get(`${API_BASE}/ai/netease`, {
        params: { link: neteaseUrl }
      })

      console.log('[网易云解析] 响应成功:', response.data)

      if (response.data.code === 200) {
        const musicData = response.data.data
        console.log('[网易云解析] 传递给父组件:', musicData)
        // 成功获取音乐信息，传递给父组件
        onNeteaseImportSuccess(musicData)
        setNeteaseUrl('')
      } else {
        setNeteaseError(response.data.message || '解析失败')
      }
    } catch (error) {
      console.error('[网易云解析] 错误:', error)
      setNeteaseError('链接解析失败：' + (error.response?.data?.message || error.message))
    } finally {
      setNeteaseLoading(false)
    }
  }

  return (
    <div className="hero">
      <div className="container">
        {/* 主标题区 */}
        <div className="hero-content">
          <h1>让音乐有颜色</h1>
          <p>使用 AI 为你的音乐生成专业级专辑封面</p>
        </div>

        {/* 两种使用方式 */}
        <div className="usage-tabs">
          <div className="tabs-header">
            <button 
              className={`tab-btn ${activeTab === 'upload' ? 'active' : ''}`}
              onClick={() => setActiveTab('upload')}
            >
              上传本地音乐
            </button>
            <button 
              className={`tab-btn ${activeTab === 'netease' ? 'active' : ''}`}
              onClick={() => setActiveTab('netease')}
            >
              网易云链接
            </button>
          </div>

          <div className="tabs-content">
            {activeTab === 'upload' && (
              <div className="tab-panel">
                <div className="upload-hint">
                  <div className="hint-icon">♫</div>
                  <p>选择本地音频文件，系统将自动识别并分析</p>
                </div>
                <button 
                  className="btn btn-primary btn-large"
                  onClick={() => onNavigate(uploadRef, 'upload')}
                >
                  上传音乐文件 →
                </button>
              </div>
            )}

            {activeTab === 'netease' && (
              <div className="tab-panel">
                <div className="netease-input-group">
                  <input 
                    type="text" 
                    className="netease-input"
                    placeholder="粘贴网易云音乐链接，如：https://music.163.com/song?id=..."
                    value={neteaseUrl}
                    onChange={(e) => {
                      setNeteaseUrl(e.target.value)
                      setNeteaseError('')
                    }}
                    disabled={neteaseLoading}
                  />
                  <button 
                    className="btn btn-primary"
                    onClick={handleNeteaseImport}
                    disabled={neteaseLoading}
                  >
                    {neteaseLoading ? '解析中...' : '从链接导入 →'}
                  </button>
                </div>
                {neteaseError && <p className="netease-error">{neteaseError}</p>}
                <p className="netease-hint">支持网易云音乐、歌曲、专辑链接</p>
              </div>
            )}
          </div>
        </div>

        {/* 特色展示 */}
        <div className="features">
          <div className="feature">
            <div className="feature-num">01</div>
            <h3>上传音乐</h3>
            <p>支持所有常见音频格式</p>
          </div>
          <div className="feature">
            <div className="feature-num">02</div>
            <h3>AI 分析</h3>
            <p>智能识别音乐特征和氛围</p>
          </div>
          <div className="feature">
            <div className="feature-num">03</div>
            <h3>自动生成</h3>
            <p>一键生成精美封面</p>
          </div>
        </div>
      </div>
    </div>
  )
}
