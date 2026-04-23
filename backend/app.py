from flask import Flask
from config import Config
from database import db


def main():
    app = Flask(__name__)
    app.config.from_object(Config)

    db.init_app(app)

    app.run()


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("[ERROR] Server stopped by user.")
    except Exception as e:
        print(f"[ERROR] An error occurred: {e}")