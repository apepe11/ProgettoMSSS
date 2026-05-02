import csv
import os
import sys
from pathlib import Path

# Aggiunge la directory superiore a sys.path per importare app e modelli
sys.path.append(str(Path(__file__).resolve().parent.parent))

from app import app
from database import db
from models import Song

def import_from_csv(csv_path):
    print(f"\n--- Analisi file: {csv_path.name} ---")

    try:
        with open(csv_path, mode='r', encoding='latin-1') as f:
            # Leggiamo la prima riga per pulire manualmente gli header
            raw_reader = csv.reader(f)
            headers = next(raw_reader, None)

            if not headers:
                return

            # Pulizia: rimuoviamo virgolette e spazi extra dai nomi delle colonne
            clean_headers = [h.strip().replace('"', '') for h in headers]

            # Se mancano Title o Artist, questo non è il file giusto per i metadati
            if 'Title' not in clean_headers or 'Artist' not in clean_headers:
                print(f"Salto {csv_path.name}: mancano colonne Title/Artist.")
                return

            # Torniamo all'inizio del file per usare DictReader con gli header puliti
            f.seek(0)
            dict_reader = csv.DictReader(f)
            dict_reader.fieldnames = clean_headers
            next(dict_reader) # Saltiamo la riga degli header

            songs_to_add = []
            for row in dict_reader:
                # Estraiamo e puliamo i valori dalle virgolette
                allmusic_id = (row.get('Song') or '').strip().replace('"', '')
                title = (row.get('Title') or 'Unknown Title').strip().replace('"', '')
                artist = (row.get('Artist') or 'Unknown Artist').strip().replace('"', '')

                if not allmusic_id or allmusic_id == ' ':
                    continue

                file_url = f"/static/music/{allmusic_id}.mp3"

                song = Song(
                    title=title,
                    artist=artist,
                    file_url=file_url
                )
                songs_to_add.append(song)

            if songs_to_add:
                # Pulizia tabella per eliminare i vecchi record "Unknown"
                print(f"Svuoto la tabella 'songs' e importo {len(songs_to_add)} canzoni...")
                db.session.query(Song).delete()

                db.session.bulk_save_objects(songs_to_add)
                db.session.commit()
                print(f"Successo! Dati importati da {csv_path.name}")

    except Exception as e:
        print(f"Errore durante l'importazione di {csv_path.name}: {e}")
        db.session.rollback()

if __name__ == "__main__":
    with app.app_context():
        db_dir = Path(__file__).resolve().parent
        for csv_file in db_dir.glob("*.csv"):
            import_from_csv(csv_file)
