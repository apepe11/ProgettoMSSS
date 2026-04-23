from dotenv import load_dotenv
import os

load_dotenv()

class Config:
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_NAME = os.getenv("DB_NAME", "HeartMusic_database")
    DB_USER = os.getenv("DB_USER")
    DB_PASSWORD = os.getenv("DB_PASSWORD")

    if not DB_USER or not DB_PASSWORD:
        raise EnvironmentError(
            "Environment variables DB_USER and DB_PASSWORD must be set."
        )

    SQLALCHEMY_DATABASE_URI = (
        f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}"
        f"@{DB_HOST}:5432/{DB_NAME}"
    )

    SECRET_KEY = os.getenv('SECRET_KEY', "secret_key")