import os
from flask import Flask, send_from_directory
from flask_cors import CORS
from flask_mail import Mail, Message
from config import Config
from database import db

# 1. Create the app globally so Docker can see it!
app = Flask(__name__, static_url_path='/flask_static')
CORS(app)
app.config.from_object(Config)

@app.route('/static/music/<path:filename>')
def serve_music(filename):
    # Supporto per i file che potrebbero essere in sottocartelle Q1, Q2, ecc.
    # ma che in realtà si trovano direttamente nella root di static/music/
    actual_filename = os.path.basename(filename)
    music_dir = os.path.join(app.root_path, 'static', 'music')
    return send_from_directory(music_dir, actual_filename)

# Configurazione Mail (Esempio per Gmail)
app.config['MAIL_SERVER'] = 'sandbox.smtp.mailtrap.io'
app.config['MAIL_PORT'] = 2525
app.config['MAIL_USE_TLS'] = True
app.config['MAIL_USERNAME'] = '898efbd798016d' # Sostituisci con la tua mail
app.config['MAIL_PASSWORD'] = '4d01dc17f1607b'     # Sostituisci con la password generata
app.config['MAIL_DEFAULT_SENDER'] = 'hello@demomailtrap.co'

mail = Mail(app)

# 2. Initialize the database connection
db.init_app(app)

# ---> NEW: Register the API routes we just built! <---
from routes import api
app.register_blueprint(api)

# 3. Import your models so the database actually creates the tables!
with app.app_context():
    import models
    db.create_all()

    # NOVITÀ: Popolamento automatico se le tabelle sono vuote
    from models import Song, Emotion, Playlist

    # 1. Popolamento Emozioni
    if Emotion.query.count() == 0:
        print("Populating emotions...")
        emotions = ["Happy", "Sad", "Calm", "Anxious", "Energetic"]
        for i, name in enumerate(emotions, 1):
            db.session.add(Emotion(emotion_id=i, name=name))
        db.session.commit()

    # 2. Popolamento Canzoni
    if Song.query.count() == 0:
        print("Database songs empty. Triggering automatic import...")
        from importer.import_songs import import_from_csv
        from pathlib import Path
        db_dir = Path(__file__).resolve().parent / 'importer'
        csv_files = list(db_dir.glob("*.csv"))
        for csv_file in csv_files:
            import_from_csv(csv_file)

    # 3. Creazione Playlist di esempio (se non ce ne sono)
    if Playlist.query.count() == 0:
        print("Creating default playlists...")
        happy_emotion = Emotion.query.filter_by(name="Happy").first()
        if happy_emotion:
            new_playlist = Playlist(
                title="Happy Vibes",
                target_emotion_id=happy_emotion.emotion_id
            )
            # Aggiungi qualche canzone a caso alla playlist
            random_songs = Song.query.limit(5).all()
            new_playlist.songs = random_songs
            db.session.add(new_playlist)
            db.session.commit()


# 4. Run the server
if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)