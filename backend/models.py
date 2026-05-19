import uuid
from datetime import datetime
from database import db
from sqlalchemy.dialects.postgresql import UUID, JSONB

# ==========================================
# 1. USER AUTHENTICATION & CORE
# ==========================================

class User(db.Model):
    __tablename__ = 'users'
    user_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    username = db.Column(db.String(255), nullable=False)
    email = db.Column(db.String(255), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    device_id = db.Column(db.String(255))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

class Song(db.Model):
    __tablename__ = 'songs'
    song_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title = db.Column(db.String(255), nullable=False)
    artist = db.Column(db.String(255), nullable=False)
    duration_sec = db.Column(db.Integer)
    file_url = db.Column(db.String(255), nullable=False)

class Emotion(db.Model):
    __tablename__ = 'emotions'
    emotion_id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False, unique=True)

# ==========================================
# 2. PLAYLISTS & CURATION
# ==========================================

# This is an "Association Table" for the Many-to-Many relationship 
# between Playlists and Songs
playlist_songs = db.Table('playlist_songs',
    db.Column('playlist_id', UUID(as_uuid=True), db.ForeignKey('playlists.playlist_id', ondelete='CASCADE'), primary_key=True),
    db.Column('song_id', UUID(as_uuid=True), db.ForeignKey('songs.song_id', ondelete='CASCADE'), primary_key=True)
)

class Playlist(db.Model):
    __tablename__ = 'playlists'
    playlist_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title = db.Column(db.String(255), nullable=False)
    target_emotion_id = db.Column(db.Integer, db.ForeignKey('emotions.emotion_id', ondelete='SET NULL'))
    
    # Relationship to Emotion
    emotion = db.relationship('Emotion', backref='playlists')

    # This lets you easily get all songs in a playlist via Python!
    songs = db.relationship('Song', secondary=playlist_songs, lazy='subquery',
        backref=db.backref('playlists', lazy=True))

# ==========================================
# 3. SENSORS & ALGORITHM ANALYSIS
# ==========================================

class ListeningSession(db.Model):
    __tablename__ = 'listening_sessions'
    session_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id', ondelete='CASCADE'))
    song_id = db.Column(UUID(as_uuid=True), db.ForeignKey('songs.song_id', ondelete='CASCADE'))
    start_time = db.Column(db.DateTime, default=datetime.utcnow)
    avg_bpm = db.Column(db.Numeric)
    hrv_score = db.Column(db.Numeric)
    avg_eda = db.Column(db.Numeric)
    system_detected_emotion_id = db.Column(db.Integer, db.ForeignKey('emotions.emotion_id', ondelete='SET NULL'))

# ==========================================
# 4. THE FLEXIBLE GROUND TRUTH
# ==========================================

class SurveyQuestion(db.Model):
    __tablename__ = 'survey_questions'
    question_id = db.Column(db.Integer, primary_key=True)
    question_text = db.Column(db.String(255), nullable=False)
    question_type = db.Column(db.String(50), nullable=False)

class SurveyResponse(db.Model):
    __tablename__ = 'survey_responses'
    response_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id = db.Column(UUID(as_uuid=True), db.ForeignKey('listening_sessions.session_id', ondelete='CASCADE'))
    user_id = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id', ondelete='CASCADE'))
    question_id = db.Column(db.Integer, db.ForeignKey('survey_questions.question_id', ondelete='CASCADE'))
    
    # Here is your magic JSONB column from the database design!
    answer_data = db.Column(JSONB, nullable=False) 
    
    submitted_at = db.Column(db.DateTime, default=datetime.utcnow)

class SongReview(db.Model):
    __tablename__ = 'song_reviews'
    review_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id', ondelete='CASCADE'), nullable=False)
    session_id = db.Column(UUID(as_uuid=True), db.ForeignKey('listening_sessions.session_id', ondelete='SET NULL'))
    emotion_id = db.Column(db.Integer, db.ForeignKey('emotions.emotion_id', ondelete='SET NULL'))
    valence = db.Column(db.Integer, nullable=False) # Parametro 1 (Slider)
    arousal = db.Column(db.Integer, nullable=False) # Parametro 2 (Slider)
    description = db.Column(db.Text)                # Descrizione (Stringa)
    detected_emotion = db.Column(db.String(100))    # Emozione rilevata dal classificatore
    created_at = db.Column(db.DateTime, default=datetime.utcnow)


# ==========================================
# 5. RAW TRAINING DATA (EEG / HR / EDA)
# ==========================================

class EEGTrainingSample(db.Model):
    __tablename__ = 'eeg_training_samples'

    sample_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    session_id = db.Column(UUID(as_uuid=True), db.ForeignKey('listening_sessions.session_id', ondelete='CASCADE'), nullable=False)
    sample = db.Column(db.Integer, nullable=False)
    subject = db.Column(db.String(255), nullable=False)
    rating = db.Column(db.Integer, nullable=False)
    ch1 = db.Column(db.Float, nullable=False)
    ch2 = db.Column(db.Float, nullable=False)
    ch3 = db.Column(db.Float, nullable=False)
    ch4 = db.Column(db.Float, nullable=False)
    ch5 = db.Column(db.Float, nullable=False)
    ch6 = db.Column(db.Float, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)


class WearableTrainingSample(db.Model):
    __tablename__ = 'wearable_training_samples'

    sample_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    session_id = db.Column(UUID(as_uuid=True), db.ForeignKey('listening_sessions.session_id', ondelete='CASCADE'), nullable=False)
    sensor_type = db.Column(db.String(10), nullable=False)  # "hr" | "eda"
    timestamp = db.Column(db.BigInteger, nullable=False)
    value = db.Column(db.Float, nullable=False)
    subject = db.Column(db.String(255), nullable=False)
    rating = db.Column(db.Integer, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)


class UserFavorite(db.Model):
    __tablename__ = 'user_favorites'
    user_id = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id', ondelete='CASCADE'), primary_key=True)
    song_id = db.Column(UUID(as_uuid=True), db.ForeignKey('songs.song_id', ondelete='CASCADE'), primary_key=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)