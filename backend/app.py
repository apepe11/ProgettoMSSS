from flask import Flask
from config import Config
from database import db

# 1. Create the app globally so Docker can see it!
app = Flask(__name__)
app.config.from_object(Config)

# 2. Initialize the database connection
db.init_app(app)

# 3. Import your models so the database actually creates the tables!
with app.app_context():
    import models
    db.create_all()

# 4. Run the server
if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)