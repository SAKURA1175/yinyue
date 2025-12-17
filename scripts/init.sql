-- 创建用户表
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建上传文件表
CREATE TABLE uploads (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- audio, image
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建音乐信息表
CREATE TABLE music_info (
    id SERIAL PRIMARY KEY,
    upload_id INT REFERENCES uploads(id) ON DELETE SET NULL,
    title VARCHAR(255),
    artist VARCHAR(255),
    album VARCHAR(255),
    bpm INT,
    key VARCHAR(10),
    lyrics TEXT,
    duration INT, -- 秒
    source_type VARCHAR(50), -- audd, netease, upload
    source_id VARCHAR(255), -- API 返回的 ID
    raw_json JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建 AI 解析结果表
CREATE TABLE ai_analysis (
    id SERIAL PRIMARY KEY,
    music_id INT NOT NULL REFERENCES music_info(id) ON DELETE CASCADE,
    theme VARCHAR(500),
    mood VARCHAR(500),
    visual_style TEXT,
    colors JSONB, -- 颜色推荐 JSON 数组
    analysis_prompt TEXT, -- 用于生图的完整 Prompt
    raw_response JSONB, -- LLM 原始响应
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建生成的图片表
CREATE TABLE generated_images (
    id SERIAL PRIMARY KEY,
    music_id INT NOT NULL REFERENCES music_info(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    image_base64 LONGTEXT, -- 存储 Base64 编码的图片
    prompt_used TEXT, -- 使用的 Prompt
    generation_model VARCHAR(50), -- stable-diffusion, flux 等
    generation_time INT, -- 生成耗时（秒）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户收藏表
CREATE TABLE user_collections (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    music_id INT NOT NULL REFERENCES music_info(id) ON DELETE CASCADE,
    image_id INT NOT NULL REFERENCES generated_images(id) ON DELETE CASCADE,
    title VARCHAR(255), -- 用户自定义的专辑名称
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, music_id)
);

-- 创建索引以提高查询性能
CREATE INDEX idx_uploads_user_id ON uploads(user_id);
CREATE INDEX idx_music_info_upload_id ON music_info(upload_id);
CREATE INDEX idx_ai_analysis_music_id ON ai_analysis(music_id);
CREATE INDEX idx_generated_images_music_id ON generated_images(music_id);
CREATE INDEX idx_user_collections_user_id ON user_collections(user_id);
CREATE INDEX idx_user_collections_music_id ON user_collections(music_id);
