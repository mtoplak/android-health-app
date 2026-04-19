import json
from pathlib import Path

import numpy as np
import tensorflow as tf


RNG = np.random.default_rng(42)
CLASS_NAMES = ["NORMAL", "ELEVATED", "CRITICAL"]


def generate_boundary_samples(size: int):
    # Focus extra samples around rule thresholds where confusion is most likely.
    hr = RNG.uniform(92, 128, size)
    spo2 = RNG.uniform(88, 97, size)
    temp = RNG.uniform(37.0, 39.4, size)

    hr += RNG.normal(0, 1.8, size)
    spo2 += RNG.normal(0, 0.45, size)
    temp += RNG.normal(0, 0.1, size)

    x = np.stack([hr, spo2, temp], axis=1).astype(np.float32)
    y = np.array([classify_rule_based(h, s, t) for h, s, t in x], dtype=np.int32)
    return x, y


def classify_rule_based(hr: float, spo2: float, temp: float) -> int:
    # Keep class mapping aligned with Android app:
    # 0 NORMAL, 1 ELEVATED, 2 CRITICAL
    if hr > 120 or spo2 < 90 or temp > 39.0:
        return 2
    if hr > 100 or spo2 < 95 or temp > 37.5:
        return 1
    return 0


def generate_dataset(size: int = 8000):
    hr = RNG.uniform(40, 160, size)
    spo2 = RNG.uniform(82, 100, size)
    temp = RNG.uniform(34.0, 41.0, size)

    # Add a little noise for robustness.
    hr += RNG.normal(0, 2.5, size)
    spo2 += RNG.normal(0, 0.6, size)
    temp += RNG.normal(0, 0.15, size)

    x = np.stack([hr, spo2, temp], axis=1).astype(np.float32)
    y = np.array([classify_rule_based(h, s, t) for h, s, t in x], dtype=np.int32)

    x_boundary, y_boundary = generate_boundary_samples(size // 3)
    x = np.concatenate([x, x_boundary], axis=0)
    y = np.concatenate([y, y_boundary], axis=0)
    return x, y


def stratified_split(x: np.ndarray, y: np.ndarray, train_ratio: float = 0.8):
    train_idx = []
    val_idx = []
    for class_id in range(len(CLASS_NAMES)):
        idx = np.where(y == class_id)[0]
        RNG.shuffle(idx)
        split_at = int(train_ratio * len(idx))
        train_idx.extend(idx[:split_at])
        val_idx.extend(idx[split_at:])

    train_idx = np.array(train_idx)
    val_idx = np.array(val_idx)
    RNG.shuffle(train_idx)
    RNG.shuffle(val_idx)
    return x[train_idx], x[val_idx], y[train_idx], y[val_idx]


def build_model(normalizer: tf.keras.layers.Normalization) -> tf.keras.Model:
    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(3,), dtype=tf.float32),
            normalizer,
            tf.keras.layers.Dense(16, activation="relu"),
            tf.keras.layers.Dense(8, activation="relu"),
            tf.keras.layers.Dense(3, activation="softmax"),
        ]
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=["accuracy"],
    )
    return model


def main():
    root = Path(__file__).resolve().parents[1]
    assets_dir = root / "app" / "src" / "main" / "assets"
    assets_dir.mkdir(parents=True, exist_ok=True)

    x, y = generate_dataset()

    # Stratified split keeps class proportions consistent.
    x_train, x_val, y_train, y_val = stratified_split(x, y, train_ratio=0.8)

    normalizer = tf.keras.layers.Normalization(axis=-1)
    normalizer.adapt(x_train)

    class_counts = np.bincount(y_train, minlength=len(CLASS_NAMES))
    class_weight = {
        class_idx: float(len(y_train) / (len(CLASS_NAMES) * count))
        for class_idx, count in enumerate(class_counts)
        if count > 0
    }

    model = build_model(normalizer)
    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss",
            patience=8,
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=3,
            min_lr=1e-5,
        ),
    ]

    model.fit(
        x_train,
        y_train,
        validation_data=(x_val, y_val),
        epochs=60,
        batch_size=64,
        class_weight=class_weight,
        callbacks=callbacks,
        verbose=2,
    )

    val_loss, val_acc = model.evaluate(x_val, y_val, verbose=0)
    print(f"Validation accuracy: {val_acc:.4f}, loss: {val_loss:.4f}")

    val_probs = model.predict(x_val, verbose=0)
    val_pred = np.argmax(val_probs, axis=1)
    cm = tf.math.confusion_matrix(y_val, val_pred, num_classes=len(CLASS_NAMES)).numpy()
    print("Confusion matrix (rows=true, cols=pred):")
    print(cm)
    for class_idx, class_name in enumerate(CLASS_NAMES):
        denom = int(np.sum(cm[class_idx]))
        class_acc = float(cm[class_idx, class_idx] / denom) if denom > 0 else 0.0
        print(f"Class accuracy {class_name:8s}: {class_acc:.4f} ({cm[class_idx, class_idx]}/{denom})")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = []
    tflite_model = converter.convert()

    model_path = assets_dir / "health_classifier.tflite"
    model_path.write_bytes(tflite_model)
    print(f"Saved TFLite model to: {model_path}")

    metadata = {
        "class_order": CLASS_NAMES,
        "input": ["srcniUtrip", "spO2", "temperatura"],
        "input_shape": [1, 3],
        "dtype": "float32",
        "notes": "Trained on synthetic data labeled by task rules.",
    }
    (assets_dir / "health_classifier.meta.json").write_text(
        json.dumps(metadata, indent=2), encoding="utf-8"
    )
    print(f"Saved metadata to: {assets_dir / 'health_classifier.meta.json'}")


if __name__ == "__main__":
    main()

