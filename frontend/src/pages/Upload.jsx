import { useState } from 'react'
import axios from 'axios'
import './Upload.css'

const API_BASE = 'http://localhost:8081/api'

export default function Upload({ onUploadSuccess, onNavigate }) {
  const [isDragging, setIsDragging] = useState(false)
  const [file, setFile] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('')

  const handleDragOver = (e) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setIsDragging(false)
    const droppedFile = e.dataTransfer.files[0]
    validateAndSetFile(droppedFile)
  }

  const handleFileSelect = (e) => {
    const selectedFile = e.target.files[0]
    validateAndSetFile(selectedFile)
  }

  const validateAndSetFile = (selectedFile) => {
    if (selectedFile && selectedFile.type.startsWith('audio/')) {
      setFile(selectedFile)
      setMessage('')
    } else {
      setMessage('请选择有效的音频文件')
      setMessageType('error')
    }
  }

  const handleUpload = async () => {
    if (!file) {
      setMessage('请先选择文件')
      setMessageType('error')
      return
    }

    try {
      setIsLoading(true)
      setProgress(0)
      setMessage('正在上传和处理...')
      setMessageType('')

      const formData = new FormData()
      formData.append('file', file)

      const response = await axios.post(
        `${API_BASE}/pipeline/full-pipeline`,
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (progressEvent) => {
            setProgress(Math.round((progressEvent.loaded / progressEvent.total) * 100))
          }
        }
      )

      if (response.data.code === 200) {
        setMessage('✓ 处理成功！')
        setMessageType('success')
        setFile(null)
        onUploadSuccess(response.data.data)
      } else {
        setMessage(response.data.message || '处理失败')
        setMessageType('error')
      }
    } catch (error) {
      console.error('上传错误:', error)
      setMessage(`错误: ${error.message}`)
      setMessageType('error')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="upload">
      <h1>上传音频文件</h1>
      
      <div className="upload-card">
        <div
          className={`upload-area ${isDragging ? 'dragging' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <div className="upload-icon">📤</div>
          <h2>拖拽音频文件到此</h2>
          <p>或点击下方选择文件</p>
          
          <input
            type="file"
            accept="audio/*"
            onChange={handleFileSelect}
            id="file-input"
            style={{ display: 'none' }}
          />
          
          <button
            className="btn btn-secondary"
            onClick={() => document.getElementById('file-input').click()}
          >
            选择文件
          </button>
        </div>

        {file && (
          <div className="file-info">
            <div className="file-icon">🎵</div>
            <div className="file-details">
              <p className="file-name">{file.name}</p>
              <p className="file-size">
                {(file.size / 1024 / 1024).toFixed(2)} MB
              </p>
            </div>
            <button
              className="btn btn-secondary"
              onClick={() => setFile(null)}
              disabled={isLoading}
            >
              ✕
            </button>
          </div>
        )}

        {message && (
          <div className={`message ${messageType}`}>
            {message}
          </div>
        )}

        {isLoading && (
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress}%` }}></div>
            <span className="progress-text">{progress}%</span>
          </div>
        )}

        <div className="upload-actions">
          <button
            className="btn btn-primary"
            onClick={handleUpload}
            disabled={!file || isLoading}
          >
            {isLoading ? '处理中...' : '开始处理'}
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => onNavigate('home')}
          >
            返回首页
          </button>
        </div>
      </div>
    </div>
  )
}
