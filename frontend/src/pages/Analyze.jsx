import './Analyze.css'

export default function Analyze({ musicData, onAnalysisComplete, onNavigate }) {
  // TODO: 从后端获取AI分析结果
  const analysis = {
    theme: '爱情',
    mood: '温柔、浪漫',
    visual_style: '柔光、梦幻',
    colors: ['#FF69B4', '#FFB6C1', '#FFC0CB'],
    image_prompt_en: 'A dreamy romantic album cover with soft pink and rose colors, pastel aesthetic, romantic mood'
  }

  const handleGenerateImage = () => {
    onAnalysisComplete(analysis)
  }

  return (
    <div className="analyze">
      <h1>AI 音乐分析</h1>
      
      <div className="analyze-layout">
        {/* 左侧：歌曲信息 */}
        <div className="analyze-left">
          <div className="card">
            <div className="card-header">歌曲信息</div>
            <div className="music-info">
              <div className="info-item">
                <label>歌名</label>
                <p>{musicData?.title || '-'}</p>
              </div>
              <div className="info-item">
                <label>歌手</label>
                <p>{musicData?.artist || '-'}</p>
              </div>
              <div className="info-item">
                <label>专辑</label>
                <p>{musicData?.album || '-'}</p>
              </div>
            </div>
          </div>
        </div>

        {/* 中间：AI分析结果 */}
        <div className="analyze-center">
          <div className="card">
            <div className="card-header">AI 分析结果</div>
            <div className="analysis-result">
              <div className="result-item">
                <h4>主题</h4>
                <p>{analysis.theme}</p>
              </div>
              <div className="result-item">
                <h4>氛围</h4>
                <p>{analysis.mood}</p>
              </div>
              <div className="result-item">
                <h4>视觉风格</h4>
                <p>{analysis.visual_style}</p>
              </div>
              <div className="result-item">
                <h4>配色方案</h4>
                <div className="color-palette">
                  {analysis.colors.map((color, idx) => (
                    <div key={idx} className="color-item" style={{ backgroundColor: color }} title={color}></div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 右侧：编辑Prompt */}
        <div className="analyze-right">
          <div className="card">
            <div className="card-header">自定义 Prompt</div>
            <textarea
              className="textarea"
              defaultValue={analysis.image_prompt_en}
              placeholder="编辑生成图片的提示词..."
            ></textarea>
            <button className="btn btn-primary" onClick={handleGenerateImage} style={{ width: '100%', marginTop: '12px' }}>
              生成封面
            </button>
            <button className="btn btn-secondary" onClick={() => onNavigate('upload')} style={{ width: '100%', marginTop: '8px' }}>
              返回上传
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
