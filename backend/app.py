from flask import Flask
from flask_cors import CORS
from flask_mail import Mail, Message
from config import Config
from database import db

# 1. Create the app globally so Docker can see it!
app = Flask(__name__)
CORS(app)
app.config.from_object(Config)

# Configurazione Mail (Esempio per Gmail)
app.config['MAIL_SERVER'] = 'smtp.gmail.com'
app.config['MAIL_PORT'] = 587
app.config['MAIL_USE_TLS'] = True
app.config['MAIL_USERNAME'] = 'la-tua-mail@gmail.com' # Sostituisci con la tua mail
app.config['MAIL_PASSWORD'] = 'tua-app-password'     # Sostituisci con la password generata
app.config['MAIL_DEFAULT_SENDER'] = 'la-tua-mail@gmail.com'

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

    # NOVITÀ: Popolamento automatico se la tabella songs è vuota
    from models import Song
    if Song.query.count() == 0:
        print("Database songs empty. Triggering automatic import...")
        from importer.import_songs import import_from_csv
        from pathlib import Path
        db_dir = Path(__file__).resolve().parent / 'importer'
        csv_files = list(db_dir.glob("*.csv"))
        for csv_file in csv_files:
            import_from_csv(csv_file)

# 4. Run the server
if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)