import { useState, useRef } from 'react'
import './App.css'
import Hero from './sections/Hero'
import Upload from './sections/Upload'
import Analysis from './sections/Analysis'
import Generate from './sections/Generate'
import Player from './components/Player'

export default function App() {
  const [activeTab, setActiveTab] = useState('home')
  const [musicData, setMusicData] = useState(null)
  const [neteaseData, setNeteaseData] = useState(null)
  const [analysis, setAnalysis] = useState(null)
  const [generatedImage, setGeneratedImage] = useState(null)
  const [audioUrl, setAudioUrl] = useState(null)

  const heroRef = useRef(null)
  const uploadRef = useRef(null)
  const analysisRef = useRef(null)
  const generateRef = useRef(null)

  const handleScroll = (ref, tab) => {
    setActiveTab(tab)
    ref.current?.scrollIntoView({ behavior: 'smooth' })
  }

  const handleNeteaseImportSuccess = (data) => {
    setMusicData(data)
    setNeteaseData(data)
    // 直接跳转到分析页面
    setActiveTab('analysis')
    setTimeout(() => analysisRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
  }

  const handleUploadSuccess = (data) => {
    setMusicData(data)
    // 保持当前的 audioUrl 不变，因为用户刚刚上传的文件就是这个
    // 如果需要切换到服务器路径，可以这里处理，但本地 blob 链接响应更快
    setActiveTab('analysis')
    setTimeout(() => analysisRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
  }

  const handleAnalysisComplete = (analysisData) => {
    setAnalysis(analysisData)
    setActiveTab('generate')
    setTimeout(() => generateRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
  }

  return (
    <div className="app">
      {/* 顶部导航 */}
      <nav className="navbar">
        <div className="nav-container">
          <div className="nav-logo">
            <span className="logo-icon">♪</span> AI 音乐专辑生成
          </div>
          <div className="nav-links">
            <a onClick={() => handleScroll(heroRef, 'home')} className={activeTab === 'home' ? 'active' : ''}>
              首页
            </a>
            <a onClick={() => handleScroll(uploadRef, 'upload')} className={activeTab === 'upload' ? 'active' : ''}>
              上传
            </a>
            {musicData && (
              <a onClick={() => handleScroll(analysisRef, 'analysis')} className={activeTab === 'analysis' ? 'active' : ''}>
                分析
              </a>
            )}
            <a onClick={() => handleScroll(generateRef, 'generate')} className={activeTab === 'generate' ? 'active' : ''}>
              生成
            </a>
          </div>
        </div>
      </nav>

      {/* 各个区域 */}
      <section ref={heroRef} className="section hero-section">
        <Hero onNavigate={(ref, tab) => handleScroll(ref, tab)} uploadRef={uploadRef} onNeteaseImportSuccess={handleNeteaseImportSuccess} />
      </section>

      <section ref={uploadRef} className="section upload-section">
        <Upload
          onUploadSuccess={handleUploadSuccess}
          neteaseData={neteaseData}
          onClearNetease={() => setNeteaseData(null)}
          onAudioSelected={setAudioUrl}
        />
      </section>

      {musicData && (
        <section ref={analysisRef} className="section analysis-section">
          <Analysis musicData={musicData} onAnalysisComplete={handleAnalysisComplete} />
        </section>
      )}

      <section ref={generateRef} className="section generate-section">
        <Generate analysis={analysis} onImageGenerated={setGeneratedImage} />
      </section>

      {/* 全局播放器 */}
      <Player audioUrl={audioUrl} />
    </div>
  )
}
