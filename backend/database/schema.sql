-- ==========================================
-- 1. USER AUTHENTICATION & CORE
-- ==========================================

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE songs (
    song_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    duration_sec INT,
    file_url VARCHAR(255) NOT NULL
);

CREATE TABLE emotions (
    emotion_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ==========================================
-- 2. PLAYLISTS & CURATION
-- ==========================================

CREATE TABLE playlists (
    playlist_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    target_emotion_id INT REFERENCES emotions (emotion_id) ON DELETE SET NULL
);

CREATE TABLE playlist_songs (
    playlist_id UUID REFERENCES playlists (playlist_id) ON DELETE CASCADE,
    song_id UUID REFERENCES songs (song_id) ON DELETE CASCADE,
    PRIMARY KEY (playlist_id, song_id)
);

-- ==========================================
-- 3. SENSORS & ALGORITHM ANALYSIS
-- ==========================================

CREATE TABLE listening_sessions (
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

CREATE TABLE survey_questions (
    question_id INT PRIMARY KEY,
    question_text VARCHAR(255) NOT NULL,
    question_type VARCHAR(50) NOT NULL
);

CREATE TABLE survey_responses (
    response_id UUID PRIMARY KEY,
    session_id UUID REFERENCES listening_sessions (session_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users (user_id) ON DELETE CASCADE,
    question_id INT REFERENCES survey_questions (question_id) ON DELETE CASCADE,
    answer_data JSONB NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);