from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Tuple

import numpy as np
import pandas as pd
import tensorflow as tf
from scipy.signal import firwin
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
)
from sklearn.model_selection import GroupShuffleSplit
from sklearn.utils.class_weight import compute_class_weight
from tensorflow.keras.constraints import max_norm
from tensorflow.keras.layers import (
    Activation,
    AveragePooling2D,
    BatchNormalization,
    Concatenate,
    Conv2D,
    Dense,
    DepthwiseConv2D,
    Dropout,
    Flatten,
    Input,
    Lambda,
    SeparableConv2D,
)
from tensorflow.keras.models import Model


# -----------------------
# Constants / schema
# -----------------------
EEG_CHANNELS = ["ch1", "ch2", "ch3", "ch4", "ch5", "ch6"]

FS_EEG = 500
FS_TARGET = 125
EPOCH_SEC = 2

EEG_SAMPLES_PER_EPOCH = FS_EEG * EPOCH_SEC  # 1000
AUX_SAMPLES_PER_EPOCH = FS_TARGET * EPOCH_SEC  # 250
NUM_CLASSES = 5


@dataclass
class DatasetBundle:
    x_eeg: np.ndarray
    x_hr: np.ndarray
    y: np.ndarray
    groups: np.ndarray


def normalize_rating_to_zero_based(rating_series: pd.Series) -> pd.Series:
    """
    Normalizes ratings to [0..4].
    Accepts either already-zero-based [0..4] or one-based [1..5].
    """
    values = rating_series.astype(int)
    unique = set(values.unique().tolist())

    if unique.issubset({0, 1, 2, 3, 4}):
        return values
    if unique.issubset({1, 2, 3, 4, 5}):
        return values - 1

    raise ValueError(
        f"Unsupported rating set: {sorted(unique)}. Expected [0..4] or [1..5]."
    )


def load_eeg_epochs(eeg_csv: Path) -> Dict[Tuple[int, str, int], Tuple[np.ndarray, int]]:
    df = pd.read_csv(eeg_csv)

    required = {"experiment", "sample", "subject", "rating", *EEG_CHANNELS}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"Missing EEG columns: {missing}")

    df["rating"] = normalize_rating_to_zero_based(df["rating"])
    df["epoch_idx"] = (df["sample"] // EEG_SAMPLES_PER_EPOCH).astype(int)

    out: Dict[Tuple[int, str, int], Tuple[np.ndarray, int]] = {}

    for (exp, subj, epoch_idx), grp in df.groupby(["experiment", "subject", "epoch_idx"]):
        grp = grp.sort_values("sample")
        if len(grp) != EEG_SAMPLES_PER_EPOCH:
            continue

        x = grp[EEG_CHANNELS].to_numpy(dtype=np.float32).T  # (6, 1000)
        y = int(grp["rating"].iloc[0])
        out[(int(exp), str(subj), int(epoch_idx))] = (x[..., np.newaxis], y)  # (6,1000,1)

    return out


def _resample_single_wearable_group(values_df: pd.DataFrame) -> np.ndarray:
    values_df = values_df.sort_values("timestamp")

    t_in = values_df["timestamp"].to_numpy(dtype=np.float64)
    v_in = values_df["value"].to_numpy(dtype=np.float32)

    if len(t_in) < 2:
        return np.array([], dtype=np.float32)

    start = t_in.min()
    end = t_in.max()

    if end <= start:
        return np.array([], dtype=np.float32)

    n_out = int(np.floor((end - start) / 1000.0 * FS_TARGET)) + 1
    if n_out < AUX_SAMPLES_PER_EPOCH:
        return np.array([], dtype=np.float32)

    t_uniform = np.linspace(start, end, num=n_out, dtype=np.float64)
    v_uniform = np.interp(t_uniform, t_in, v_in).astype(np.float32)
    return v_uniform


def load_wearable_epochs(wearable_csv: Path) -> Dict[Tuple[int, str, int], Tuple[np.ndarray, int]]:
    df = pd.read_csv(wearable_csv)

    required = {"experiment", "timestamp", "value", "subject", "rating"}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"Missing wearable columns in {wearable_csv.name}: {missing}")

    df["rating"] = normalize_rating_to_zero_based(df["rating"])

    out: Dict[Tuple[int, str, int], Tuple[np.ndarray, int]] = {}

    for (exp, subj), grp in df.groupby(["experiment", "subject"]):
        v_uniform = _resample_single_wearable_group(grp)
        if v_uniform.size == 0:
            continue

        n_epochs = v_uniform.size // AUX_SAMPLES_PER_EPOCH
        if n_epochs <= 0:
            continue

        rating = int(grp["rating"].iloc[0])

        for epoch_idx in range(n_epochs):
            s = epoch_idx * AUX_SAMPLES_PER_EPOCH
            e = s + AUX_SAMPLES_PER_EPOCH
            segment = v_uniform[s:e]
            if segment.size != AUX_SAMPLES_PER_EPOCH:
                continue
            x = segment[np.newaxis, :, np.newaxis]  # (1,250,1)
            out[(int(exp), str(subj), int(epoch_idx))] = (x.astype(np.float32), rating)

    return out


def build_aligned_dataset(
    eeg_map: Dict[Tuple[int, str, int], Tuple[np.ndarray, int]],
    hr_map: Dict[Tuple[int, str, int], Tuple[np.ndarray, int]],
) -> DatasetBundle:
    keys = sorted(set(eeg_map.keys()) & set(hr_map.keys()))
    if not keys:
        raise ValueError("No aligned epochs across EEG and HR. Check input files.")

    x_eeg, x_hr, y, groups = [], [], [], []

    for key in keys:
        eeg_x, eeg_y = eeg_map[key]
        hr_x, hr_y = hr_map[key]

        # label consistency check
        if eeg_y != hr_y:
            continue

        exp, _subj, _epoch = key
        x_eeg.append(eeg_x)
        x_hr.append(hr_x)
        y.append(eeg_y)
        groups.append(exp)  # session-level grouping to prevent leakage

    if not x_eeg:
        raise ValueError("All aligned samples were dropped by label mismatch checks.")

    return DatasetBundle(
        x_eeg=np.stack(x_eeg).astype(np.float32),
        x_hr=np.stack(x_hr).astype(np.float32),
        y=np.array(y, dtype=np.int64),
        groups=np.array(groups, dtype=np.int64),
    )


def split_grouped(bundle: DatasetBundle, random_state: int = 42):
    """
    Leakage-resistant split by experiment/session group.
    - test = 20% groups
    - val  = 10% of remaining groups
    """
    idx = np.arange(bundle.y.shape[0])

    gss1 = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=random_state)
    train_val_idx, test_idx = next(gss1.split(idx, bundle.y, groups=bundle.groups))

    gss2 = GroupShuffleSplit(n_splits=1, test_size=0.10, random_state=random_state)
    tr_rel_idx, val_rel_idx = next(
        gss2.split(
            train_val_idx,
            bundle.y[train_val_idx],
            groups=bundle.groups[train_val_idx],
        )
    )

    train_idx = train_val_idx[tr_rel_idx]
    val_idx = train_val_idx[val_rel_idx]

    def take(indices: np.ndarray) -> DatasetBundle:
        return DatasetBundle(
            x_eeg=bundle.x_eeg[indices],
            x_hr=bundle.x_hr[indices],
            y=bundle.y[indices],
            groups=bundle.groups[indices],
        )

    return take(train_idx), take(val_idx), take(test_idx)


def apply_fir(x, coeffs):
    """Applies 1D FIR along temporal axis for each channel."""
    x2 = tf.squeeze(x, -1)  # (B,Ch,T)
    x2 = tf.expand_dims(x2, 1)  # (B,1,T,Ch)

    k = tf.constant(coeffs.reshape(1, -1, 1, 1), tf.float32)
    n_ch = x2.shape[-1]
    k = tf.tile(k, [1, 1, n_ch, 1])  # (1,K,Ch,1)

    y = tf.nn.depthwise_conv2d(
        x2,
        k,
        strides=[1, 1, 1, 1],
        padding="SAME",
        data_format="NHWC",
    )

    y = tf.squeeze(y, 1)  # (B,T,Ch)
    y = tf.transpose(y, [0, 2, 1])  # (B,Ch,T)
    return y[..., tf.newaxis]  # (B,Ch,T,1)


def build_model(channel_means: np.ndarray, channel_stds: np.ndarray) -> Model:
    # Fixed FIR preprocessing, reproducible in TFLite
    numtaps = 101
    hp_coeffs = firwin(numtaps, cutoff=0.5, fs=FS_EEG, pass_zero=False)
    bp_coeffs = firwin(numtaps, cutoff=[0.5, 50.0], fs=FS_EEG, pass_zero=False)
    bs_coeffs = firwin(numtaps, cutoff=[49.0, 51.0], fs=FS_EEG, pass_zero=True)
    aa_coeffs = firwin(numtaps, cutoff=FS_TARGET / 2, fs=FS_EEG)

    dec_factor = FS_EEG // FS_TARGET
    kern_length = int(0.5 * FS_TARGET)

    eeg_in = Input(shape=(6, EEG_SAMPLES_PER_EPOCH, 1), name="eeg_input")
    hr_in = Input(shape=(1, AUX_SAMPLES_PER_EPOCH, 1), name="hr_input")

    x = Lambda(lambda z: apply_fir(z, hp_coeffs), name="hp")(eeg_in)
    x = Lambda(lambda z: apply_fir(z, bp_coeffs), name="bp")(x)
    x = Lambda(lambda z: apply_fir(z, bs_coeffs), name="notch")(x)
    x = Lambda(lambda z: apply_fir(z, aa_coeffs), name="aa")(x)
    x = Lambda(lambda z: z[:, :, ::dec_factor, :], name="decimate")(x)

    merged = Concatenate(axis=1)([x, hr_in])  # (B,7,250,1)
    chans = 7

    means = tf.constant(channel_means.reshape(1, -1, 1, 1), dtype=tf.float32)
    stds = tf.constant(np.clip(channel_stds, 1e-6, None).reshape(1, -1, 1, 1), dtype=tf.float32)
    norm = Lambda(lambda z: (z - means) / stds, name="normalize")(merged)

    # EEGNet block 1
    b1 = Conv2D(8, (1, kern_length), padding="same", use_bias=False)(norm)
    b1 = BatchNormalization()(b1)
    b1 = AveragePooling2D((1, 4))(b1)
    b1 = Dropout(0.5)(b1)

    # EEGNet block 2
    b2 = SeparableConv2D(16, (1, 16), padding="same", use_bias=False)(b1)
    b2 = BatchNormalization()(b2)
    b2 = Activation("elu")(b2)
    b2 = AveragePooling2D((1, 8))(b2)
    b2 = Dropout(0.5)(b2)

    flat = Flatten(name="flatten")(b2)
    logits = Dense(NUM_CLASSES, kernel_constraint=max_norm(0.25), name="dense")(flat)
    out = Activation("softmax", name="softmax")(logits)

    model = Model(inputs=[eeg_in, hr_in], outputs=out, name="EmotionEEGNet")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def evaluate_split(model: Model, split: DatasetBundle, split_name: str) -> Dict:
    probs = model.predict(
        {
            "eeg_input": split.x_eeg,
            "hr_input": split.x_hr,
        },
        verbose=0,
    )
    pred = np.argmax(probs, axis=1)

    metrics = {
        "split": split_name,
        "accuracy": float(accuracy_score(split.y, pred)),
        "balanced_accuracy": float(balanced_accuracy_score(split.y, pred)),
        "f1_macro": float(f1_score(split.y, pred, average="macro", zero_division=0)),
        "confusion_matrix": confusion_matrix(split.y, pred).tolist(),
        "classification_report": classification_report(
            split.y, pred, output_dict=True, zero_division=0
        ),
    }
    return metrics


def make_channel_stats(train_split: DatasetBundle) -> Tuple[np.ndarray, np.ndarray]:
    # compute on training only (criticality fix: no train/test leakage)
    x_merged = np.concatenate([train_split.x_eeg, train_split.x_hr], axis=1)
    means = x_merged.mean(axis=(0, 2, 3))
    stds = x_merged.std(axis=(0, 2, 3))
    return means.astype(np.float32), stds.astype(np.float32)


def export_tflite(model: Model, output_path: Path) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    output_path.write_bytes(tflite_model)


def main():
    parser = argparse.ArgumentParser(description="Train multimodal emotion classifier")
    parser.add_argument("--eeg_csv", type=Path, required=True)
    parser.add_argument("--eeg_fs", type=int, default=FS_EEG)
    parser.add_argument("--aux_fs", type=int, default=FS_TARGET)
    parser.add_argument("--epoch_sec", type=int, default=EPOCH_SEC)
    parser.add_argument("--hr_csv", type=Path, required=True)
    parser.add_argument("--out_dir", type=Path, default=Path("models"))
    parser.add_argument("--epochs", type=int, default=250)
    parser.add_argument("--batch_size", type=int, default=64)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    np.random.seed(args.seed)
    tf.random.set_seed(args.seed)

    args.out_dir.mkdir(parents=True, exist_ok=True)

    global FS_EEG, FS_TARGET, EPOCH_SEC, EEG_SAMPLES_PER_EPOCH, AUX_SAMPLES_PER_EPOCH
    FS_EEG = args.eeg_fs
    FS_TARGET = args.aux_fs
    EPOCH_SEC = args.epoch_sec
    EEG_SAMPLES_PER_EPOCH = FS_EEG * EPOCH_SEC
    AUX_SAMPLES_PER_EPOCH = FS_TARGET * EPOCH_SEC

    eeg_map = load_eeg_epochs(args.eeg_csv)
    hr_map = load_wearable_epochs(args.hr_csv)

    bundle = build_aligned_dataset(eeg_map, hr_map)
    train_split, val_split, test_split = split_grouped(bundle, random_state=args.seed)

    channel_means, channel_stds = make_channel_stats(train_split)
    model = build_model(channel_means, channel_stds)

    classes = np.unique(train_split.y)
    class_weights_raw = compute_class_weight(class_weight="balanced", classes=classes, y=train_split.y)
    class_weight = {int(c): float(w) for c, w in zip(classes, class_weights_raw)}

    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss", patience=25, restore_best_weights=True
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss", factor=0.5, patience=8, min_lr=1e-6
        ),
    ]

    history = model.fit(
        {
            "eeg_input": train_split.x_eeg,
            "hr_input": train_split.x_hr,
        },
        train_split.y,
        validation_data=(
            {
                "eeg_input": val_split.x_eeg,
                "hr_input": val_split.x_hr,
            },
            val_split.y,
        ),
        epochs=args.epochs,
        batch_size=args.batch_size,
        class_weight=class_weight,
        callbacks=callbacks,
        verbose=1,
    )

    train_metrics = evaluate_split(model, train_split, "train")
    val_metrics = evaluate_split(model, val_split, "val")
    test_metrics = evaluate_split(model, test_split, "test")

    keras_path = args.out_dir / "emotion_eegnet.keras"
    tflite_path = args.out_dir / "emotion_eegnet.tflite"
    metadata_path = args.out_dir / "emotion_eegnet_metadata.json"

    model.save(keras_path)
    export_tflite(model, tflite_path)

    metadata = {
        "label_space": "0..4",
        "label_mapping": {
            "0": "very_negative",
            "1": "negative",
            "2": "neutral",
            "3": "positive",
            "4": "very_positive",
        },
        "input_shapes": {
            "eeg_input": [6, EEG_SAMPLES_PER_EPOCH, 1],
            "hr_input": [1, AUX_SAMPLES_PER_EPOCH, 1],
        },
        "sampling": {
            "eeg_fs": FS_EEG,
            "aux_fs": FS_TARGET,
            "epoch_sec": EPOCH_SEC,
        },
        "class_weight": class_weight,
        "channel_means": channel_means.tolist(),
        "channel_stds": channel_stds.tolist(),
        "num_samples": {
            "train": int(train_split.y.size),
            "val": int(val_split.y.size),
            "test": int(test_split.y.size),
        },
        "metrics": {
            "train": train_metrics,
            "val": val_metrics,
            "test": test_metrics,
        },
        "history": {
            k: [float(vv) for vv in vals] for k, vals in history.history.items()
        },
        "criticality_fixes": [
            "rating normalized to 0..4 to avoid label-space mismatch",
            "group/session-level split to reduce epoch leakage",
            "normalization stats fit on train split only",
            "class weighting for class imbalance",
            "early stopping + LR scheduling",
        ],
    }

    metadata_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")

    print("Training completed.")
    print(f"Saved Keras model:  {keras_path}")
    print(f"Saved TFLite model: {tflite_path}")
    print(f"Saved metadata:     {metadata_path}")
    print(f"Test accuracy:      {test_metrics['accuracy']:.4f}")
    print(f"Test macro F1:      {test_metrics['f1_macro']:.4f}")


if __name__ == "__main__":
    main()
