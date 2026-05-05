import os
import sys
from pathlib import Path

import psycopg2
from dotenv import load_dotenv
from mutagen.mp3 import MP3


def get_env(name: str, default: str | None = None) -> str:
    value = os.getenv(name, default)
    if not value:
        raise RuntimeError(f"Missing environment variable: {name}")
    return value


def resolve_audio_path(static_dir: Path, file_url: str) -> Path:
    # file_url is stored like "/static/music/MT0000004637.mp3"
    filename = Path(file_url).name
    return static_dir / filename


def main() -> int:
    load_dotenv()

    db_host = get_env("DB_HOST", "localhost")
    db_name = get_env("DB_NAME", "HeartMusic_database")
    db_user = get_env("DB_USER")
    db_password = get_env("DB_PASSWORD")

    base_dir = Path(__file__).resolve().parents[1]
    static_dir = base_dir / "static" / "music"
    if not static_dir.exists():
        print(f"Static directory not found: {static_dir}")
        return 1

    conn = psycopg2.connect(
        host=db_host,
        dbname=db_name,
        user=db_user,
        password=db_password,
        port=5432,
    )
    conn.autocommit = False

    updated = 0
    skipped = 0
    missing = 0

    try:
        with conn.cursor() as cur:
            cur.execute("SELECT song_id, file_url FROM songs ORDER BY song_id")
            rows = cur.fetchall()

            for song_id, file_url in rows:
                if not file_url:
                    skipped += 1
                    continue

                audio_path = resolve_audio_path(static_dir, file_url)
                if not audio_path.exists():
                    missing += 1
                    continue

                try:
                    audio = MP3(audio_path)
                    duration_sec = int(round(audio.info.length))
                except Exception:
                    skipped += 1
                    continue

                cur.execute(
                    "UPDATE songs SET duration_sec = %s WHERE song_id = %s",
                    (duration_sec, str(song_id)),
                )
                updated += 1

        conn.commit()
    except Exception as exc:
        conn.rollback()
        print(f"Error updating durations: {exc}")
        return 1
    finally:
        conn.close()

    print(
        f"Updated: {updated}, Skipped: {skipped}, Missing files: {missing}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
