from flask import Blueprint, request, jsonify, send_from_directory
from flask_mail import Message
from werkzeug.security import generate_password_hash, check_password_hash
from pathlib import Path
import csv
import os
from database import db
from sqlalchemy import or_, func, desc, text
from models import (
    ListeningSession,
    SurveyResponse,
    Playlist,
    User,
    SurveyQuestion,
    EEGTrainingSample,
    WearableTrainingSample,
    SongReview,
    Song,
    Emotion,
    UserFavorite
)

# Create a 'Blueprint' to hold all our URLs
api = Blueprint('api', __name__)



def send_welcome_email(user_email, username):
    from app import mail # Import circolare gestito dentro la funzione
    msg = Message('Welcome to HeartMusic!',
                  recipients=[user_email])
    msg.body = f"Hi {username}! Thank you for registering to HeartMusic. Start listening to your heart!"
    try:
        mail.send(msg)
    except Exception as e:
        print(f"Error sending email: {e}")

# ==========================================
# 0. USER AUTHENTICATION
# ==========================================
@api.route('/api/users/register', methods=['POST'], strict_slashes=False)
def register_user():
    data = request.get_json()
    
    # Safety check: Is this email already in the database?
    if User.query.filter_by(email=data.get('email')).first():
        return jsonify({"error": "Email already registered"}), 400
        
    # Securely encrypt the password before saving
    hashed_password = generate_password_hash(data.get('password'))
    
    new_user = User(
        username=data.get('username'),
        email=data.get('email'),
        password_hash=hashed_password,
        device_id=data.get('device_id')
    )
    
    db.session.add(new_user)
    db.session.commit()

    # Invia la mail di benvenuto
    send_welcome_email(new_user.email, new_user.username)

    return jsonify({"message": "User created successfully!", "user_id": new_user.user_id}), 201

@api.route('/api/users/login', methods=['POST'], strict_slashes=False)
def login_user():
    data = request.get_json()
    identifier = data.get('email') # Può essere email o username

    # Cerca l'utente sia per email che per username
    user = User.query.filter((User.email == identifier) | (User.username == identifier)).first()
    
    # If the user exists AND the encrypted passwords match
    if user and check_password_hash(user.password_hash, data.get('password')):
        return jsonify({
            "message": "Login successful", 
            "user_id": user.user_id, 
            "username": user.username
        }), 200
        
    return jsonify({"error": "Invalid email or password"}), 401

# ==========================================
# 1. RECEIVE SMARTWATCH DATA (BPM & EDA)
# ==========================================
@api.route('/api/sessions', methods=['POST'])
def save_watch_data():
    data = request.get_json()
    
    new_session = ListeningSession(
        user_id=data.get('user_id'),
        song_id=data.get('song_id'),
        avg_bpm=data.get('avg_bpm'),
        avg_eda=data.get('avg_eda')
    )
    
    db.session.add(new_session)
    db.session.commit()
    
    return jsonify({
        "message": "Smartwatch data saved successfully!",
        "session_id": str(new_session.session_id)
    }), 201

# ==========================================
# 2. SURVEYS: GET QUESTIONS & SAVE ANSWERS
# ==========================================
@api.route('/api/questions', methods=['GET'])
def get_questions():
    # Grab all questions from the database
    questions = SurveyQuestion.query.all()
    
    # Format them into a neat JSON list for the phone
    q_list = []
    for q in questions:
        q_list.append({
            "question_id": q.question_id, 
            "text": q.question_text, 
            "type": q.question_type
        })
        
    return jsonify({"questions": q_list}), 200

@api.route('/api/surveys', methods=['POST'])
def save_survey():
    data = request.get_json()
    
    new_survey = SurveyResponse(
        session_id=data.get('session_id'),
        user_id=data.get('user_id'),
        question_id=data.get('question_id'),
        answer_data=data.get('answer_data') # Expects JSON format
    )
    
    db.session.add(new_survey)
    db.session.commit()
    
    return jsonify({"message": "Survey response saved!"}), 201

# ==========================================
# 3. PLAYLISTS & SONGS
# ==========================================

@api.route('/api/playlists/<int:emotion_id>', methods=['GET'])
def get_playlist_by_emotion(emotion_id):
    playlist = Playlist.query.filter_by(target_emotion_id=emotion_id).first()

    if not playlist:
        return jsonify({"error": "No playlist found for this emotion"}), 404

    song_list = []
    for song in playlist.songs:
        song_list.append({
            "song_id": str(song.song_id),
            "title": song.title,
            "artist": song.artist,
            "url": song.file_url
        })

    return jsonify({
        "playlist_title": playlist.title,
        "songs": song_list
    }), 200

@api.route('/api/playlists', methods=['GET'])
def get_playlists():
    query = request.args.get('q', '')

    if not query:
        # If no query, return all playlists
        playlists = Playlist.query.all()
    else:
        # Search by title, emotion name or song title
        search_filter = or_(
            Playlist.title.ilike(f'%{query}%'),
            Emotion.name.ilike(f'%{query}%'),
            Song.title.ilike(f'%{query}%')
        )
        playlists = Playlist.query.outerjoin(Emotion).outerjoin(Playlist.songs).filter(search_filter).distinct().all()

    results = []
    for p in playlists:
        results.append({
            "playlist_id": str(p.playlist_id),
            "title": p.title,
            "emotion": p.emotion.name if p.emotion else "General"
        })

    return jsonify({"playlists": results}), 200

@api.route('/api/playlists/<uuid:playlist_id>', methods=['GET'])
def get_playlist_details(playlist_id):
    playlist = Playlist.query.get(playlist_id)

    if not playlist:
        return jsonify({"error": "Playlist not found"}), 404

    song_list = []
    for song in playlist.songs:
        song_list.append({
            "song_id": str(song.song_id),
            "title": song.title,
            "artist": song.artist,
            "url": song.file_url,
            "duration": song.duration_sec
        })

    return jsonify({
        "playlist_id": str(playlist.playlist_id),
        "title": playlist.title,
        "emotion": playlist.emotion.name if playlist.emotion else "General",
        "songs": song_list
    }), 200

@api.route('/api/songs/<uuid:song_id>', methods=['GET'])
def get_song_details(song_id):
    """Get details for a single song"""
    song = Song.query.get(song_id)
    if not song:
        return jsonify({"error": "Song not found"}), 404

    return jsonify({
        "song_id": str(song.song_id),
        "title": song.title,
        "artist": song.artist,
        "url": song.file_url,
        "duration": song.duration_sec
    }), 200

@api.route('/api/songs', methods=['GET'])
def get_songs():
    """Search for songs by title or artist"""
    query = request.args.get('q', '')

    if not query:
        # If no query, return some songs
        songs = Song.query.limit(20).all()
    else:
        # Search by title or artist
        search_filter = or_(
            Song.title.ilike(f'%{query}%'),
            Song.artist.ilike(f'%{query}%')
        )
        songs = Song.query.filter(search_filter).limit(50).all()

    results = []
    for s in songs:
        results.append({
            "song_id": str(s.song_id),
            "title": s.title,
            "artist": s.artist,
            "url": s.file_url,
            "duration": s.duration_sec
        })

    return jsonify({"songs": results}), 200

# ==========================================
# 4. GET USER HISTORY
# ==========================================
@api.route('/api/users/<uuid:user_id>/sessions', methods=['GET'])
def get_user_history(user_id):
    # Fetch sessions for this user, ordered by newest first
    sessions = ListeningSession.query.filter_by(user_id=user_id).order_by(ListeningSession.start_time.desc()).all()
    
    session_list = []
    for s in sessions:
        session_list.append({
            "session_id": s.session_id,
            "song_id": s.song_id,
            "start_time": s.start_time,
            # Convert Decimals to float so JSON can read them
            "avg_bpm": float(s.avg_bpm) if s.avg_bpm else None,
            "avg_eda": float(s.avg_eda) if s.avg_eda else None
        })
        
    return jsonify({"history": session_list}), 200


# ==========================================
# 4. REAL-TIME SENSOR DATA (LIVE COLLECTION)
# ==========================================

@api.route('/api/sensors/hr', methods=['POST'])
def collect_heart_rate():
    """Receive heart rate data during live listening session"""
    data = request.get_json() or {}
    
    session_id = data.get('session_id')
    timestamp = data.get('timestamp', int(__import__('time').time() * 1000))
    value = data.get('value')
    
    if not session_id or value is None:
        return jsonify({"error": "session_id and value required"}), 400
    
    try:
        wearable = WearableTrainingSample(
            session_id=session_id,
            sensor_type='hr',
            timestamp=timestamp,
            value=float(value),
            subject='user',
            rating=0  # Will be updated later during survey
        )
        db.session.add(wearable)
        db.session.commit()
        
        return jsonify({
            "message": "Heart rate data received",
            "sample_id": wearable.sample_id
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400


@api.route('/api/sensors/eda', methods=['POST'])
def collect_eda():
    """Receive EDA data during live listening session"""
    data = request.get_json() or {}
    
    session_id = data.get('session_id')
    timestamp = data.get('timestamp', int(__import__('time').time() * 1000))
    value = data.get('value')
    
    if not session_id or value is None:
        return jsonify({"error": "session_id and value required"}), 400
    
    try:
        wearable = WearableTrainingSample(
            session_id=session_id,
            sensor_type='eda',
            timestamp=timestamp,
            value=float(value),
            subject='user',
            rating=0
        )
        db.session.add(wearable)
        db.session.commit()
        
        return jsonify({
            "message": "EDA data received",
            "sample_id": wearable.sample_id
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400


@api.route('/api/sensors/eeg', methods=['POST'])
def collect_eeg():
    """Receive EEG data during live listening session"""
    data = request.get_json() or {}
    
    session_id = data.get('session_id')
    sample_num = data.get('sample')
    
    if not session_id or sample_num is None:
        return jsonify({"error": "session_id and sample required"}), 400
    
    try:
        eeg = EEGTrainingSample(
            session_id=session_id,
            sample=sample_num,
            subject='user',
            rating=0,
            ch1=float(data.get('ch1', 0)),
            ch2=float(data.get('ch2', 0)),
            ch3=float(data.get('ch3', 0)),
            ch4=float(data.get('ch4', 0)),
            ch5=float(data.get('ch5', 0)),
            ch6=float(data.get('ch6', 0))
        )
        db.session.add(eeg)
        db.session.commit()
        
        return jsonify({
            "message": "EEG data received",
            "sample_id": eeg.sample_id
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400


# ==========================================
# 5. TRAINING DATA INGESTION & EXPORT
# ==========================================

def _normalize_rating(raw_rating):
    try:
        rating = int(raw_rating)
    except (TypeError, ValueError):
        return None

    if 0 <= rating <= 4:
        return rating
    if 1 <= rating <= 5:
        return rating - 1
    return None


@api.route('/api/training/sessions', methods=['POST'])
def create_training_session():
    data = request.get_json() or {}

    new_session = ListeningSession(
        user_id=data.get('user_id'),
        song_id=data.get('song_id'),
        avg_bpm=data.get('avg_bpm'),
        avg_eda=data.get('avg_eda')
    )
    db.session.add(new_session)
    db.session.commit()

    return jsonify({
        "message": "Training session created.",
        "session_id": str(new_session.session_id)
    }), 201


@api.route('/api/training/eeg', methods=['POST'])
def save_training_eeg_samples():
    data = request.get_json() or {}

    session_id = data.get('session_id')
    subject = (data.get('subject') or '').strip()
    rating = _normalize_rating(data.get('rating'))
    samples = data.get('samples')

    if not session_id:
        return jsonify({"error": "session_id is required"}), 400
    if not subject:
        return jsonify({"error": "subject is required"}), 400
    if rating is None:
        return jsonify({"error": "rating must be in [0..4] or [1..5]"}), 400
    if not isinstance(samples, list) or len(samples) == 0:
        return jsonify({"error": "samples must be a non-empty list"}), 400

    session = ListeningSession.query.filter_by(session_id=session_id).first()
    if not session:
        return jsonify({"error": "session_id not found"}), 404

    rows = []
    for i, sample in enumerate(samples):
        try:
            row = EEGTrainingSample(
                session_id=session.session_id,
                sample=int(sample['sample']),
                subject=subject,
                rating=rating,
                ch1=float(sample['ch1']),
                ch2=float(sample['ch2']),
                ch3=float(sample['ch3']),
                ch4=float(sample['ch4']),
                ch5=float(sample['ch5']),
                ch6=float(sample['ch6']),
            )
            rows.append(row)
        except (TypeError, ValueError, KeyError):
            return jsonify({"error": f"Invalid EEG sample at index {i}"}), 400

    db.session.bulk_save_objects(rows)
    db.session.commit()

    return jsonify({
        "message": "EEG samples saved.",
        "session_id": str(session.session_id),
        "inserted": len(rows)
    }), 201


@api.route('/api/training/wearable', methods=['POST'])
def save_training_wearable_samples():
    data = request.get_json() or {}

    session_id = data.get('session_id')
    sensor_type = (data.get('sensor_type') or '').strip().lower()
    subject = (data.get('subject') or '').strip()
    rating = _normalize_rating(data.get('rating'))
    samples = data.get('samples')

    if not session_id:
        return jsonify({"error": "session_id is required"}), 400
    if sensor_type not in {'hr', 'eda'}:
        return jsonify({"error": "sensor_type must be 'hr' or 'eda'"}), 400
    if not subject:
        return jsonify({"error": "subject is required"}), 400
    if rating is None:
        return jsonify({"error": "rating must be in [0..4] or [1..5]"}), 400
    if not isinstance(samples, list) or len(samples) == 0:
        return jsonify({"error": "samples must be a non-empty list"}), 400

    session = ListeningSession.query.filter_by(session_id=session_id).first()
    if not session:
        return jsonify({"error": "session_id not found"}), 404

    rows = []
    for i, sample in enumerate(samples):
        try:
            row = WearableTrainingSample(
                session_id=session.session_id,
                sensor_type=sensor_type,
                timestamp=int(sample['timestamp']),
                value=float(sample['value']),
                subject=subject,
                rating=rating,
            )
            rows.append(row)
        except (TypeError, ValueError, KeyError):
            return jsonify({"error": f"Invalid wearable sample at index {i}"}), 400

    db.session.bulk_save_objects(rows)
    db.session.commit()

    return jsonify({
        "message": f"{sensor_type.upper()} samples saved.",
        "session_id": str(session.session_id),
        "inserted": len(rows)
    }), 201


@api.route('/api/training/export', methods=['POST'])
def export_training_data_for_classifier():
    """
    Export DB data into the CSV schema expected by emotion_classifier/train_emotion_classifier.py
    """
    payload = request.get_json(silent=True) or {}

    default_out_dir = Path(__file__).resolve().parent / 'training_exports' / 'raw'
    out_dir = Path(payload.get('out_dir', str(default_out_dir))).expanduser().resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    eeg_path = out_dir / 'eeg_data.csv'
    hr_path = out_dir / 'hr_data.csv'
    eda_path = out_dir / 'eda_data.csv'

    # Build stable integer experiment IDs from sessions that contain training data
    session_ids = set(
        r.session_id for r in EEGTrainingSample.query.with_entities(EEGTrainingSample.session_id).all()
    ) | set(
        r.session_id for r in WearableTrainingSample.query.with_entities(WearableTrainingSample.session_id).all()
    )

    if not session_ids:
        return jsonify({"error": "No training data available to export"}), 400

    ordered_sessions = (
        ListeningSession.query
        .filter(ListeningSession.session_id.in_(session_ids))
        .order_by(ListeningSession.start_time.asc())
        .all()
    )
    exp_map = {s.session_id: i + 1 for i, s in enumerate(ordered_sessions)}

    eeg_rows = (
        EEGTrainingSample.query
        .order_by(EEGTrainingSample.session_id.asc(), EEGTrainingSample.sample.asc())
        .all()
    )
    hr_rows = (
        WearableTrainingSample.query
        .filter_by(sensor_type='hr')
        .order_by(WearableTrainingSample.session_id.asc(), WearableTrainingSample.timestamp.asc())
        .all()
    )
    eda_rows = (
        WearableTrainingSample.query
        .filter_by(sensor_type='eda')
        .order_by(WearableTrainingSample.session_id.asc(), WearableTrainingSample.timestamp.asc())
        .all()
    )

    with eeg_path.open('w', newline='', encoding='utf-8') as f:
        w = csv.writer(f)
        w.writerow(['experiment', 'sample', 'subject', 'rating', 'ch1', 'ch2', 'ch3', 'ch4', 'ch5', 'ch6'])
        for r in eeg_rows:
            if r.session_id not in exp_map:
                continue
            w.writerow([
                exp_map[r.session_id],
                r.sample,
                r.subject,
                r.rating,
                r.ch1,
                r.ch2,
                r.ch3,
                r.ch4,
                r.ch5,
                r.ch6,
            ])

    def _write_wearable_csv(path: Path, rows):
        with path.open('w', newline='', encoding='utf-8') as f:
            w = csv.writer(f)
            w.writerow(['experiment', 'timestamp', 'value', 'subject', 'rating'])
            for r in rows:
                if r.session_id not in exp_map:
                    continue
                w.writerow([
                    exp_map[r.session_id],
                    r.timestamp,
                    r.value,
                    r.subject,
                    r.rating,
                ])

    _write_wearable_csv(hr_path, hr_rows)
    _write_wearable_csv(eda_path, eda_rows)

    return jsonify({
        "message": "Training CSV export completed.",
        "out_dir": str(out_dir),
        "files": {
            "eeg_csv": str(eeg_path),
            "hr_csv": str(hr_path),
            "eda_csv": str(eda_path),
        },
        "counts": {
            "eeg_rows": len(eeg_rows),
            "hr_rows": len(hr_rows),
            "eda_rows": len(eda_rows),
            "experiments": len(exp_map),
        }
    }), 200

# ==========================================
# 6. SONG REVIEWS (YOUR FEELINGS)
# ==========================================

@api.route('/api/reviews', methods=['POST'])
def save_song_review():
    data = request.get_json()

    try:
        new_review = SongReview(
            user_id=data.get('user_id'),
            valence=int(data.get('valence')),
            arousal=int(data.get('arousal')),
            description=data.get('description'),
            detected_emotion=data.get('detected_emotion')
        )
        db.session.add(new_review)
        db.session.commit()

        return jsonify({
            "message": "Review saved successfully!",
            "review_id": str(new_review.review_id)
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@api.route('/api/users/<uuid:user_id>/reviews', methods=['GET'])
def get_user_reviews(user_id):
    reviews = SongReview.query.filter_by(user_id=user_id).order_by(SongReview.created_at.desc()).all()

    review_list = []
    for r in reviews:
        review_list.append({
            "review_id": str(r.review_id),
            "valence": r.valence,
            "arousal": r.arousal,
            "description": r.description,
            "detected_emotion": r.detected_emotion,
            "created_at": r.created_at.isoformat()
        })

    return jsonify({"reviews": review_list}), 200

# ==========================================
# 7. SYSTEM: IMPORT SONGS
# ==========================================

@api.route('/api/system/import-songs', methods=['POST'])
def trigger_song_import():
    """Trigger the CSV import process"""
    from importer.import_songs import import_from_csv
    from pathlib import Path

    db_dir = Path(__file__).resolve().parent / 'importer'
    csv_files = list(db_dir.glob("*.csv"))

    if not csv_files:
        return jsonify({"error": "No CSV files found in backend/database/"}), 404

    try:
        for csv_file in csv_files:
            import_from_csv(csv_file)
        return jsonify({"message": "Import completed successfully"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    

@api.route('/api/insights', methods=['GET'])
def get_insights():
    # 1. Query for APP DETECTED totals
    # Joins ListeningSession and Emotion tables based on system_detected_emotion_id
    app_detected_counts = db.session.query(
        Emotion.name, db.func.count(ListeningSession.session_id)
    ).join(ListeningSession, ListeningSession.system_detected_emotion_id == Emotion.emotion_id) \
     .group_by(Emotion.name).all()

    # 2. Query for USER EXPERIENCED totals
    # Since user experience is stored in the SurveyResponse JSONB column, 
    # we extract the 'emotion_id' directly from the JSON payload.
    # Note: This assumes your Android app sends survey data like {"emotion_id": 1, ...}
    user_experienced_counts = db.session.query(
        Emotion.name, db.func.count(SurveyResponse.response_id)
    ).join(
        SurveyResponse, 
        db.cast(SurveyResponse.answer_data['emotion_id'].astext, db.Integer) == Emotion.emotion_id
    ).group_by(Emotion.name).all()

    # 3. Calculate percentages and send it to Android as JSON
    return jsonify({
        "app_detected": calculate_percentages(app_detected_counts),
        "user_experienced": calculate_percentages(user_experienced_counts)
    })

# Helper function to turn raw counts into 0.0 - 1.0 percentages
def calculate_percentages(counts_list):
    total = sum([count for name, count in counts_list])
    if total == 0:
        return {"happy": 0, "sad": 0, "calm": 0, "anxious": 0, "energetic": 0}
    
    # Create a dictionary of percentages
    stats = {name.lower(): (count / total) for name, count in counts_list}
    return stats   


@api.route('/api/songs/top', methods=['GET'])
def get_top_songs():
    try:
        # Uniamo le canzoni con i preferiti, raggruppiamo per ID canzone e ordiniamo per il conteggio
        top_songs_query = db.session.query(
            Song, func.count(UserFavorite.song_id).label('likes_count')
        ).outerjoin(UserFavorite, Song.song_id == UserFavorite.song_id)\
         .group_by(Song.song_id)\
         .order_by(desc('likes_count'))\
         .limit(20).all()

        results = []
        for song, count in top_songs_query:
            results.append({
                "song_id": str(song.song_id),
                "title": song.title,
                "artist": song.artist,
                "url": song.file_url,
                "likes": count
            })
        return jsonify({"songs": results}), 200
    except Exception as e:
        print(f"Error in get_top_songs: {e}")
        return jsonify({"songs": [], "error": str(e)}), 500


@api.route('/api/favorites/toggle', methods=['POST'])
def toggle_favorite():
    data = request.json
    user_id = data.get('user_id')
    song_id = data.get('song_id')

    if not user_id or not song_id:
        return jsonify({"error": "Missing user_id or song_id"}), 400

    try:
        # Check if the favorite already exists
        check_query = text("SELECT * FROM user_favorites WHERE user_id = :u_id AND song_id = :s_id")
        result = db.session.execute(check_query, {"u_id": user_id, "s_id": song_id}).fetchone()

        if result:
            # It exists! This means the user is UN-LIKING the song
            delete_query = text("DELETE FROM user_favorites WHERE user_id = :u_id AND song_id = :s_id")
            db.session.execute(delete_query, {"u_id": user_id, "s_id": song_id})
            db.session.commit()
            return jsonify({"message": "Song removed from favorites", "is_favorite": False}), 200
        else:
            # It doesn't exist! This means the user is LIKING the song
            insert_query = text("INSERT INTO user_favorites (user_id, song_id) VALUES (:u_id, :s_id)")
            db.session.execute(insert_query, {"u_id": user_id, "s_id": song_id})
            db.session.commit()
            return jsonify({"message": "Song added to favorites", "is_favorite": True}), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


# 2. GET USER'S FAVORITE SONGS
@api.route('/api/favorites/<user_id>', methods=['GET'])
def get_user_favorites(user_id):
    try:
        # Join the songs table with the user_favorites table to get full song details
        query = text("""
            SELECT s.song_id, s.title, s.artist, s.file_url 
            FROM songs s
            JOIN user_favorites uf ON s.song_id = uf.song_id
            WHERE uf.user_id = :u_id
            ORDER BY uf.created_at DESC
        """)
        
        results = db.session.execute(query, {"u_id": user_id}).fetchall()
        
        # Format the data into a JSON list
        favorite_songs = []
        for row in results:
            favorite_songs.append({
                "songId": str(row.song_id),
                "title": row.title,
                "artist": row.artist,
                "url": row.file_url,
                "likes": 1 # You can update this later if you want total global likes
            })
            
        return jsonify(favorite_songs), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500
