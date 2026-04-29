import csv
import os
import uuid
import sys
from pathlib import Path

# Add the parent directory to sys.path so we can import app and models
sys.path.append(str(Path(__file__).resolve().parent.parent))

from app import app
from database import db
from models import Song

def import_from_csv(csv_path):
    print(f"Starting import from {csv_path}...")

    # Usiamo 'latin-1' che è molto permissiva per i caratteri speciali
    # ed evita i crash di codifica che bloccano l'avvio del server.
    try:
        with open(csv_path, mode='r', encoding='latin-1') as f:
            reader = csv.DictReader(f)
            songs_to_add = []

            for row in reader:
                # Determine the local file URL
                # Expected structure: static/music/Qx/SongID.mp3
                quadrant = row.get('Quadrant', 'Q1')
                allmusic_id = row.get('Song', '')
                filename = f"{allmusic_id}.mp3"
                file_url = f"/static/music/{quadrant}/{filename}"

                # Create Song object
                song = Song(
                    allmusic_id=allmusic_id,
                    title=row.get('Title', 'Unknown Title'),
                    artist=row.get('Artist', 'Unknown Artist'),
                    quadrant=quadrant,
                    pquad=float(row.get('PQuad', 0)) if row.get('PQuad') else 0,
                    genres=row.get('GenresStr', ''),
                    moods=row.get('MoodsStr', ''),
                    file_url=file_url
                )
                songs_to_add.append(song)

            if songs_to_add:
                db.session.bulk_save_objects(songs_to_add)
                db.session.commit()
                print(f"Successfully imported {len(songs_to_add)} songs!")
            else:
                print("No songs found in CSV.")
    except Exception as e:
        print(f"Error during import: {e}")
        db.session.rollback()

if __name__ == "__main__":
    with app.app_context():
        # Look for any CSV file in this directory
        db_dir = Path(__file__).resolve().parent
        csv_files = list(db_dir.glob("*.csv"))

        if not csv_files:
            print(f"No CSV files found in {db_dir}")
        else:
            for csv_file in csv_files:
                import_from_csv(csv_file)
