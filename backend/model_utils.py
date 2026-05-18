from pathlib import Path
import pickle
from typing import Dict, List, Optional, Tuple

import numpy as np
from sklearn.dummy import DummyClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score

from database import db
from models import EEGTrainingSample, ListeningSession, WearableTrainingSample

MODEL_DIR = Path(__file__).resolve().parent / "model"
MAX_EEG_SAMPLES = 10000
MODEL_FILE = MODEL_DIR / "emotion_predictor.pkl"
SCALER_FILE = MODEL_DIR / "feature_scaler.npz"

CLASS_LABELS = ["Sad", "Anxious", "Calm", "Energetic", "Happy"]
EMOTION_NAME_TO_ID = {
    "Happy": 1,
    "Sad": 2,
    "Calm": 3,
    "Anxious": 4,
    "Energetic": 5,
}
CLASS_TO_EMOTION_ID = {i: EMOTION_NAME_TO_ID[name] for i, name in enumerate(CLASS_LABELS)}


def _normalize_rating(raw_rating) -> Optional[int]:
    try:
        rating = int(raw_rating)
    except (TypeError, ValueError):
        return None

    if 0 <= rating <= 4:
        return rating
    if 1 <= rating <= 5:
        return rating - 1
    return None


def _ensure_model_dir() -> None:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)


def _extract_session_features(session_id: str) -> Optional[Tuple[np.ndarray, int]]:
    eeg_rows = (
        EEGTrainingSample.query
        .filter_by(session_id=session_id)
        .order_by(EEGTrainingSample.sample.asc())
        .all()
    )
    hr_rows = (
        WearableTrainingSample.query
        .filter_by(session_id=session_id, sensor_type='hr')
        .order_by(WearableTrainingSample.timestamp.asc())
        .all()
    )

    if not eeg_rows or not hr_rows:
        return None

    label = _normalize_rating(eeg_rows[0].rating)
    if label is None:
        return None

    if len(eeg_rows) > MAX_EEG_SAMPLES:
        step = int(np.ceil(len(eeg_rows) / MAX_EEG_SAMPLES))
        eeg_rows = eeg_rows[::step]

    eeg_values = np.array([
        [row.ch1, row.ch2, row.ch3, row.ch4, row.ch5, row.ch6]
        for row in eeg_rows
    ], dtype=np.float32)
    hr_values = np.array([row.value for row in hr_rows], dtype=np.float32)

    if eeg_values.size == 0 or hr_values.size == 0:
        return None

    features: List[float] = []
    for channel_idx in range(eeg_values.shape[1]):
        channel_values = eeg_values[:, channel_idx]
        features.extend([
            float(np.mean(channel_values)),
            float(np.std(channel_values)),
            float(np.min(channel_values)),
            float(np.max(channel_values)),
        ])

    features.extend([
        float(np.mean(hr_values)),
        float(np.std(hr_values)),
        float(np.min(hr_values)),
        float(np.max(hr_values)),
        float(hr_values.size),
    ])

    return np.array(features, dtype=np.float32), label


def _load_training_dataset() -> Tuple[np.ndarray, np.ndarray, List[str]]:
    session_ids = {
        r.session_id for r in EEGTrainingSample.query.with_entities(EEGTrainingSample.session_id).all()
    } & {
        r.session_id for r in WearableTrainingSample.query.filter_by(sensor_type='hr').with_entities(WearableTrainingSample.session_id).all()
    }

    feature_rows: List[np.ndarray] = []
    labels: List[int] = []
    valid_sessions: List[str] = []

    for session_id in sorted(session_ids):
        extracted = _extract_session_features(str(session_id))
        if extracted is None:
            continue
        features, label = extracted
        feature_rows.append(features)
        labels.append(label)
        valid_sessions.append(str(session_id))

    if len(feature_rows) == 0:
        raise ValueError("No complete HR + EEG sessions available for training.")

    X = np.vstack(feature_rows)
    y = np.array(labels, dtype=np.int64)
    return X, y, valid_sessions


def train_emotion_model() -> Dict:
    _ensure_model_dir()

    X, y, valid_sessions = _load_training_dataset()

    unique_labels, label_counts = np.unique(y, return_counts=True)
    can_stratify = len(unique_labels) > 1 and np.all(label_counts >= 2)
    stratify = y if can_stratify else None
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42,
        stratify=stratify,
    )

    feature_mean = X_train.mean(axis=0)
    feature_std = X_train.std(axis=0)
    feature_std[feature_std == 0] = 1.0

    X_train_scaled = (X_train - feature_mean) / feature_std
    X_test_scaled = (X_test - feature_mean) / feature_std

    if len(np.unique(y_train)) < 2:
        model = DummyClassifier(strategy='most_frequent')
    else:
        model = LogisticRegression(
            multi_class='multinomial',
            solver='lbfgs',
            max_iter=1000,
            random_state=42,
        )
    model.fit(X_train_scaled, y_train)

    y_train_pred = model.predict(X_train_scaled)
    y_test_pred = model.predict(X_test_scaled)

    train_accuracy = float(accuracy_score(y_train, y_train_pred))
    test_accuracy = float(accuracy_score(y_test, y_test_pred))

    with open(MODEL_FILE, 'wb') as handle:
        pickle.dump(model, handle)

    np.savez(SCALER_FILE, mean=feature_mean, std=feature_std)

    metrics = {
        'sessions_trained': len(valid_sessions),
        'feature_dimension': X.shape[1],
        'train_accuracy': train_accuracy,
        'test_accuracy': test_accuracy,
        'class_labels': CLASS_LABELS,
    }
    return metrics


def _load_model() -> Tuple[LogisticRegression, np.ndarray, np.ndarray]:
    if not MODEL_FILE.exists() or not SCALER_FILE.exists():
        raise FileNotFoundError('Trained model not found')

    with open(MODEL_FILE, 'rb') as handle:
        model = pickle.load(handle)

    scaler_data = np.load(SCALER_FILE)
    feature_mean = scaler_data['mean']
    feature_std = scaler_data['std']
    return model, feature_mean, feature_std


def predict_emotion_for_session(session_id: str, train_if_missing: bool = True) -> Dict:
    if train_if_missing and (not MODEL_FILE.exists() or not SCALER_FILE.exists()):
        train_emotion_model()

    model, feature_mean, feature_std = _load_model()
    extracted = _extract_session_features(str(session_id))
    if extracted is None:
        raise ValueError('Session does not contain enough EEG and HR data for prediction.')

    features, _ = extracted
    scaled = (features - feature_mean) / feature_std
    predicted = int(model.predict(scaled.reshape(1, -1))[0])

    if hasattr(model, 'predict_proba'):
        probabilities = model.predict_proba(scaled.reshape(1, -1))[0]
        confidence = float(np.max(probabilities))
    else:
        confidence = 0.0

    emotion_name = CLASS_LABELS[predicted]
    emotion_id = CLASS_TO_EMOTION_ID.get(predicted, None)

    return {
        'predicted_class': int(predicted),
        'predicted_emotion': emotion_name,
        'emotion_id': emotion_id,
        'confidence': confidence,
    }


def get_emotion_id_for_label(label: str) -> Optional[int]:
    return EMOTION_NAME_TO_ID.get(label)
