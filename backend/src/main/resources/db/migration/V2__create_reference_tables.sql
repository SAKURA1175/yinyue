CREATE TABLE IF NOT EXISTS cover_reference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  artist VARCHAR(255) NOT NULL,
  release_year INT NULL,
  genre VARCHAR(100) NULL,
  mood_tags JSON NULL,
  style_tags JSON NULL,
  color_palette JSON NULL,
  composition_tags JSON NULL,
  source_url VARCHAR(512) NULL,
  image_storage_key VARCHAR(512) NOT NULL,
  license_tag VARCHAR(50) NOT NULL DEFAULT 'unknown',
  is_enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_cover_reference_genre (genre),
  INDEX idx_cover_reference_enabled (is_enabled),
  INDEX idx_cover_reference_year (release_year)
);

CREATE TABLE IF NOT EXISTS reference_query_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  upload_id BIGINT NULL,
  request_json JSON NOT NULL,
  topk INT NOT NULL,
  latency_ms INT NOT NULL,
  result_ids JSON NOT NULL,
  risk_flags JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_reference_query_log_upload (upload_id),
  INDEX idx_reference_query_log_created (created_at)
);

CREATE TABLE IF NOT EXISTS generation_risk_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  generation_record_id BIGINT NULL,
  upload_id BIGINT NULL,
  reference_ids JSON NULL,
  max_similarity_score DECIMAL(6,4) NULL,
  risk_level VARCHAR(20) NOT NULL,
  hit_rules JSON NULL,
  action_taken VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_generation_risk_upload (upload_id),
  INDEX idx_generation_risk_level (risk_level),
  INDEX idx_generation_risk_created (created_at)
);
