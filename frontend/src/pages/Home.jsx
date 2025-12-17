import { useState } from 'react'
import './Home.css'

export default function Home({ onNavigate }) {
  const [neteaseLinkInput, setNeteaseLinkInput] = useState('')

  const handleNeteaseLinkSubmit = () => {
    if (neteaseLinkInput.trim()) {
      // TODO: 调用网易云解析API
      console.log('解析网易云链接:', neteaseLinkInput)
      onNavigate('analyze', { type: 'netease', url: neteaseLinkInput })
    }
  }

  return (
    <div className="home">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-content">
          <h1>🎵 AI 音乐专辑生成</h1>
          <p>将你的音乐转化为精美的艺术作品</p>
        </div>
      </section>

      {/* 两种方式 */}
      <section className="methods">
        {/* 方式1: 上传音频 */}
        <div className="method-card">
          <div className="method-icon">📤</div>
          <h2>上传音频</h2>
          <p>上传你的音乐文件，自动识别歌曲信息和风格</p>
          <button className="btn btn-primary" onClick={() => onNavigate('upload')}>
            开始上传
          </button>
        </div>

        {/* 方式2: 网易云链接 */}
        <div className="method-card">
          <div className="method-icon">🔗</div>
          <h2>网易云链接</h2>
          <p>输入网易云音乐链接，自动获取歌曲信息</p>
          <div className="netease-input-group">
            <input
              type="text"
              className="input"
              placeholder="粘贴网易云链接..."
              value={neteaseLinkInput}
              onChange={(e) => setNeteaseLinkInput(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleNeteaseLinkSubmit()}
            />
            <button className="btn btn-primary" onClick={handleNeteaseLinkSubmit}>
              解析
            </button>
          </div>
        </div>
      </section>

      {/* 功能特性 */}
      <section className="features">
        <h2>功能特性</h2>
        <div className="features-grid">
          <div className="feature-item">
            <div className="feature-icon">🎼</div>
            <h3>智能识别</h3>
            <p>自动识别歌曲信息、BPM、音乐风格</p>
          </div>
          <div className="feature-item">
            <div className="feature-icon">🤖</div>
            <h3>AI 分析</h3>
            <p>深度学习分析音乐主题和视觉风格</p>
          </div>
          <div className="feature-item">
            <div className="feature-icon">🎨</div>
            <h3>自动生成</h3>
            <p>一键生成专业级专辑封面</p>
          </div>
          <div className="feature-item">
            <div className="feature-icon">💾</div>
            <h3>保存分享</h3>
            <p>保存生成的封面，分享给朋友</p>
          </div>
        </div>
      </section>

      {/* 最近生成 */}
      <section className="recent">
        <h2>最近生成</h2>
        <div className="recent-grid">
          {/* TODO: 从数据库加载最近生成的封面 */}
          <div className="empty-state">
            <p>暂无生成记录</p>
          </div>
        </div>
      </section>
    </div>
  )
}
