CREATE TABLE IF NOT EXISTS music_track (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NULL,
    album VARCHAR(255) NULL,
    file_path VARCHAR(1024) NOT NULL,
    file_size BIGINT NULL,
    audd_result LONGTEXT NULL,
    ai_analysis LONGTEXT NULL,
    album_cover LONGBLOB NULL,
    cover_url VARCHAR(1024) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    status VARCHAR(64) NULL,
    record_type VARCHAR(64) NOT NULL DEFAULT 'TRACK'
);

CREATE INDEX idx_music_track_status_created_at ON music_track(status, created_at);
CREATE INDEX idx_music_track_record_type_created_at ON music_track(record_type, created_at);
