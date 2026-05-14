#!/usr/bin/env python3
"""
Import offline Fitbit/Google Takeout cEDA samples into wearable_training_samples.

The importer maps exported cEDA timestamps to listening session windows:
session.start_time <= sample_time <= session.start_time + song.duration_sec.
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from dataclasses import dataclass
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Any, Iterable
from zoneinfo import ZoneInfo

BACKEND_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_DIR))

from app import app  # noqa: E402
from database import db  # noqa: E402
from models import ListeningSession, Song, WearableTrainingSample  # noqa: E402


@dataclass(frozen=True)
class CedaSample:
    timestamp_ms: int
    value: float


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Import Fitbit/Google Takeout cEDA samples for training."
    )
    parser.add_argument(
        "export_path",
        type=Path,
        help="Path to a Fitbit/Google Takeout export folder, CSV, or JSON file.",
    )
    parser.add_argument(
        "--session-id",
        help="Import only one listening session UUID. Default: all sessions.",
    )
    parser.add_argument(
        "--subject",
        default="user",
        help="Subject label stored in wearable_training_samples.",
    )
    parser.add_argument(
        "--rating",
        type=int,
        default=3,
        help="Training rating. Accepts 0..4 or 1..5; 1..5 is converted to 0..4.",
    )
    parser.add_argument(
        "--input-timezone",
        default="Europe/Rome",
        help="Timezone for naive export timestamps. Default: Europe/Rome.",
    )
    parser.add_argument(
        "--default-duration-sec",
        type=int,
        default=35,
        help="Fallback duration when songs.duration_sec is missing.",
    )
    parser.add_argument(
        "--pad-before-sec",
        type=float,
        default=0.0,
        help="Include cEDA samples this many seconds before session start.",
    )
    parser.add_argument(
        "--pad-after-sec",
        type=float,
        default=0.0,
        help="Include cEDA samples this many seconds after song end.",
    )
    parser.add_argument(
        "--replace-existing",
        action="store_true",
        help="Delete existing EDA samples for matched sessions before importing.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be imported without writing to the database.",
    )
    return parser.parse_args()


def normalize_rating(raw: int) -> int:
    if 0 <= raw <= 4:
        return raw
    if 1 <= raw <= 5:
        return raw - 1
    raise ValueError("--rating must be in [0..4] or [1..5]")


def discover_export_files(path: Path) -> list[Path]:
    if path.is_file():
        return [path]

    patterns = ("*ceda*", "*eda*", "*body*response*", "*body_response*")
    files: list[Path] = []
    for pattern in patterns:
        files.extend(
            p
            for p in path.rglob(pattern)
            if p.is_file() and p.suffix.lower() in {".csv", ".json"}
        )

    seen = set()
    unique = []
    for file in files:
        if file not in seen:
            seen.add(file)
            unique.append(file)
    return unique


def parse_timestamp(raw: Any, naive_tz: ZoneInfo) -> int | None:
    if raw is None:
        return None

    if isinstance(raw, (int, float)):
        value = float(raw)
        if value > 10_000_000_000:
            return int(value)
        if value > 10_000_000:
            return int(value * 1000)
        return None

    text = str(raw).strip()
    if not text:
        return None

    try:
        numeric = float(text)
        return parse_timestamp(numeric, naive_tz)
    except ValueError:
        pass

    normalized = text.replace("Z", "+00:00")
    for fmt in (None, "%Y-%m-%d %H:%M:%S", "%m/%d/%y %H:%M:%S", "%m/%d/%Y %H:%M:%S"):
        try:
            dt = datetime.fromisoformat(normalized) if fmt is None else datetime.strptime(text, fmt)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=naive_tz)
            return int(dt.astimezone(timezone.utc).timestamp() * 1000)
        except ValueError:
            continue
    return None


def parse_value(raw: Any) -> float | None:
    if raw is None:
        return None
    try:
        return float(str(raw).strip())
    except ValueError:
        return None


def row_to_sample(row: dict[str, Any], naive_tz: ZoneInfo) -> CedaSample | None:
    lowered = {str(k).strip().lower(): v for k, v in row.items()}

    timestamp = None
    for key in ("timestamp", "time", "datetime", "date_time", "date time", "starttime", "start_time"):
        if key in lowered:
            timestamp = parse_timestamp(lowered[key], naive_tz)
            break

    value = None
    for key in (
        "eda_level_real",
        "eda",
        "ceda",
        "value",
        "level",
        "electrodermal_activity",
        "skin_conductance",
    ):
        if key in lowered:
            value = parse_value(lowered[key])
            break

    if timestamp is None or value is None:
        return None
    return CedaSample(timestamp, value)


def load_csv(path: Path, naive_tz: ZoneInfo) -> list[CedaSample]:
    with path.open(newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        return [sample for row in reader if (sample := row_to_sample(row, naive_tz))]


def iter_json_dicts(data: Any) -> Iterable[dict[str, Any]]:
    if isinstance(data, dict):
        yield data
        for value in data.values():
            yield from iter_json_dicts(value)
    elif isinstance(data, list):
        for item in data:
            yield from iter_json_dicts(item)


def load_json(path: Path, naive_tz: ZoneInfo) -> list[CedaSample]:
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    return [
        sample
        for item in iter_json_dicts(data)
        if (sample := row_to_sample(item, naive_tz))
    ]


def load_samples(export_path: Path, naive_tz: ZoneInfo) -> list[CedaSample]:
    files = discover_export_files(export_path)
    if not files:
        raise FileNotFoundError(f"No cEDA/EDA CSV or JSON files found under {export_path}")

    samples: list[CedaSample] = []
    for file in files:
        suffix = file.suffix.lower()
        loaded = load_csv(file, naive_tz) if suffix == ".csv" else load_json(file, naive_tz)
        if loaded:
            print(f"Loaded {len(loaded)} cEDA samples from {file}")
            samples.extend(loaded)

    deduped = {(s.timestamp_ms, s.value): s for s in samples}
    return sorted(deduped.values(), key=lambda s: s.timestamp_ms)


def session_window_ms(session: ListeningSession, default_duration_sec: int, pad_before: float, pad_after: float) -> tuple[int, int]:
    start = session.start_time
    if start.tzinfo is None:
        start = start.replace(tzinfo=timezone.utc)
    else:
        start = start.astimezone(timezone.utc)

    duration_sec = session.song.duration_sec if session.song and session.song.duration_sec else default_duration_sec
    start = start - timedelta(seconds=pad_before)
    end = start + timedelta(seconds=duration_sec + pad_before + pad_after)
    return int(start.timestamp() * 1000), int(end.timestamp() * 1000)


def main() -> int:
    args = parse_args()
    rating = normalize_rating(args.rating)
    naive_tz = ZoneInfo(args.input_timezone)
    samples = load_samples(args.export_path.expanduser().resolve(), naive_tz)

    if not samples:
        print("No parseable cEDA samples found.")
        return 1

    with app.app_context():
        query = ListeningSession.query.join(Song, ListeningSession.song_id == Song.song_id, isouter=True)
        if args.session_id:
            query = query.filter(ListeningSession.session_id == args.session_id)
        sessions = query.order_by(ListeningSession.start_time.asc()).all()

        if not sessions:
            print("No matching listening sessions found.")
            return 1

        total_inserted = 0
        total_matched = 0
        for session in sessions:
            start_ms, end_ms = session_window_ms(
                session,
                args.default_duration_sec,
                args.pad_before_sec,
                args.pad_after_sec,
            )
            matched = [s for s in samples if start_ms <= s.timestamp_ms <= end_ms]
            total_matched += len(matched)

            if args.replace_existing and not args.dry_run:
                WearableTrainingSample.query.filter_by(
                    session_id=session.session_id,
                    sensor_type="eda",
                ).delete()

            existing_ts = {
                ts
                for (ts,) in WearableTrainingSample.query
                .with_entities(WearableTrainingSample.timestamp)
                .filter_by(session_id=session.session_id, sensor_type="eda")
                .all()
            }

            rows = [
                WearableTrainingSample(
                    session_id=session.session_id,
                    sensor_type="eda",
                    timestamp=s.timestamp_ms,
                    value=s.value,
                    subject=args.subject,
                    rating=rating,
                )
                for s in matched
                if s.timestamp_ms not in existing_ts
            ]

            avg_eda = sum(s.value for s in matched) / len(matched) if matched else None
            print(
                f"{session.session_id} | song={getattr(session.song, 'title', None)!r} "
                f"| matched={len(matched)} | new={len(rows)} | avg_eda={avg_eda}"
            )

            if rows and not args.dry_run:
                db.session.bulk_save_objects(rows)
                if avg_eda is not None:
                    session.avg_eda = avg_eda
                total_inserted += len(rows)

        if not args.dry_run:
            db.session.commit()

    print(f"Matched samples: {total_matched}")
    print(f"Inserted samples: {0 if args.dry_run else total_inserted}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
