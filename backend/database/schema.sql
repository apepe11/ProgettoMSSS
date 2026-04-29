-- CREATE DATABASE IF NOT EXISTS HeartMusic_database;

-- ==========================================
-- 1. USER AUTHENTICATION & CORE
-- ==========================================

CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS songs (
    song_id UUID PRIMARY KEY,
    allmusic_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    duration_sec INT,
    file_url VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS emotions (
    emotion_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ==========================================
-- 2. PLAYLISTS & CURATION
-- ==========================================

CREATE TABLE IF NOT EXISTS playlists (
    playlist_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    target_emotion_id INT REFERENCES emotions (emotion_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS playlist_songs (
    playlist_id UUID REFERENCES playlists (playlist_id) ON DELETE CASCADE,
    song_id UUID REFERENCES songs (song_id) ON DELETE CASCADE,
    PRIMARY KEY (playlist_id, song_id)
);

-- ==========================================
-- 3. SENSORS & ALGORITHM ANALYSIS
-- ==========================================

CREATE TABLE IF NOT EXISTS listening_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users (user_id) ON DELETE CASCADE,
    song_id UUID REFERENCES songs (song_id) ON DELETE CASCADE,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    avg_bpm DECIMAL,
    hrv_score DECIMAL,
    avg_eda DECIMAL,
    system_detected_emotion_id INT REFERENCES emotions (emotion_id) ON DELETE SET NULL
);

-- ==========================================
-- 4. THE FLEXIBLE GROUND TRUTH
-- ==========================================

CREATE TABLE IF NOT EXISTS survey_questions (
    question_id INT PRIMARY KEY,
    question_text VARCHAR(255) NOT NULL,
    question_type VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS survey_responses (
    response_id UUID PRIMARY KEY,
    session_id UUID REFERENCES listening_sessions (session_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users (user_id) ON DELETE CASCADE,
    question_id INT REFERENCES survey_questions (question_id) ON DELETE CASCADE,
    answer_data JSONB NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. RAW TRAINING DATA (EEG / HR / EDA)
-- ==========================================

CREATE TABLE IF NOT EXISTS eeg_training_samples (
    sample_id BIGSERIAL PRIMARY KEY,
    session_id UUID REFERENCES listening_sessions (session_id) ON DELETE CASCADE,
    sample INT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    rating INT NOT NULL,
    ch1 DOUBLE PRECISION NOT NULL,
    ch2 DOUBLE PRECISION NOT NULL,
    ch3 DOUBLE PRECISION NOT NULL,
    ch4 DOUBLE PRECISION NOT NULL,
    ch5 DOUBLE PRECISION NOT NULL,
    ch6 DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_eeg_training_samples_session_sample
    ON eeg_training_samples (session_id, sample);

CREATE TABLE IF NOT EXISTS wearable_training_samples (
    sample_id BIGSERIAL PRIMARY KEY,
    session_id UUID REFERENCES listening_sessions (session_id) ON DELETE CASCADE,
    sensor_type VARCHAR(10) NOT NULL,
    timestamp BIGINT NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    subject VARCHAR(255) NOT NULL,
    rating INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wearable_training_samples_session_ts
    ON wearable_training_samples (session_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_wearable_training_samples_type
    ON wearable_training_samples (sensor_type);