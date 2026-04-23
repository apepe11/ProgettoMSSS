from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
from database import db
from models import ListeningSession, SurveyResponse, Playlist, User, SurveyQuestion

# Create a 'Blueprint' to hold all our URLs
api = Blueprint('api', __name__)

# ==========================================
# 0. USER AUTHENTICATION
# ==========================================
@api.route('/api/users/register', methods=['POST'])
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
    
    return jsonify({"message": "User created successfully!", "user_id": new_user.user_id}), 201

@api.route('/api/users/login', methods=['POST'])
def login_user():
    data = request.get_json()
    
    # Find the user by their email
    user = User.query.filter_by(email=data.get('email')).first()
    
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
    
    return jsonify({"message": "Smartwatch data saved successfully!"}), 201

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
# 3. SEND PLAYLISTS TO THE PHONE
# ==========================================
@api.route('/api/playlists/<int:emotion_id>', methods=['GET'])
def get_playlist(emotion_id):
    playlist = Playlist.query.filter_by(target_emotion_id=emotion_id).first()
    
    if not playlist:
        return jsonify({"error": "No playlist found for this emotion"}), 404
        
    song_list = []
    for song in playlist.songs:
        song_list.append({
            "title": song.title,
            "artist": song.artist,
            "url": song.file_url
        })
        
    return jsonify({
        "playlist_title": playlist.title,
        "songs": song_list
    }), 200

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