import React, { useState } from 'react'
import axios from 'axios'
import './UploadSection.css'

const API_BASE = 'http://localhost:8081/api'

export default function UploadSection({ onUploadSuccess }) {
  const [isDragging, setIsDragging] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [file, setFile] = useState(null)
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('') // 'success' or 'error'

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
    if (droppedFile && droppedFile.type.startsWith('audio/')) {
      setFile(droppedFile)
      setMessage('')
    } else {
      setMessage('请上传音频文件')
      setMessageType('error')
    }
  }

  const handleFileSelect = (e) => {
    const selectedFile = e.target.files[0]
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
      setMessage('正在上传并处理...')
      setMessageType('')

      // 创建 FormData
      const formData = new FormData()
      formData.append('file', file)

      // 上传到后端
      const response = await axios.post(
        `${API_BASE}/pipeline/full-pipeline`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      if (response.data.code === 200) {
        setMessage('✓ 处理成功！')
        setMessageType('success')
        setFile(null)
        
        // 调用父组件回调
        if (onUploadSuccess) {
          onUploadSuccess(response.data.data)
        }
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
    <div className="upload-container">
      <h1 className="upload-title">🎵 AI 音乐专辑生成</h1>
      
      <div className="upload-section">
        <div
          className={`upload-box ${isDragging ? 'dragging' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <div className="upload-icon">🎤</div>
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
            className="button button-secondary"
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
              className="button button-secondary"
              onClick={() => setFile(null)}
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

        <button
          className="button button-primary"
          onClick={handleUpload}
          disabled={!file || isLoading}
          style={{ opacity: !file || isLoading ? 0.6 : 1 }}
        >
          {isLoading ? (
            <>
              <span className="spinner"></span> 处理中...
            </>
          ) : (
            '开始处理'
          )}
        </button>
      </div>
    </div>
  )
}
