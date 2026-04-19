from pathlib import Path

import numpy as np
import tensorflow as tf


CLASS_NAMES = ["NORMAL", "ELEVATED", "CRITICAL"]


def predict(interpreter: tf.lite.Interpreter, sample: np.ndarray):
	input_details = interpreter.get_input_details()
	output_details = interpreter.get_output_details()

	tensor = sample.astype(np.float32).reshape(1, 3)
	interpreter.set_tensor(input_details[0]["index"], tensor)
	interpreter.invoke()
	probs = interpreter.get_tensor(output_details[0]["index"])[0]
	idx = int(np.argmax(probs))
	return CLASS_NAMES[idx], probs


def main():
	root = Path(__file__).resolve().parents[1]
	model_path = root / "app" / "src" / "main" / "assets" / "health_classifier.tflite"

	if not model_path.exists():
		raise FileNotFoundError(f"Model not found: {model_path}")

	interpreter = tf.lite.Interpreter(model_path=str(model_path))
	interpreter.allocate_tensors()

	samples = {
		"normal": np.array([72, 98, 36.7]),
		"normal_border": np.array([98, 95.2, 37.4]),
		"elevated": np.array([108, 94, 37.9]),
		"elevated_border": np.array([101, 94.9, 37.6]),
		"critical": np.array([130, 85, 39.6]),
		"critical_border": np.array([121, 89.8, 39.1]),
	}

	for label, sample in samples.items():
		predicted, probs = predict(interpreter, sample)
		print(f"{label:8s} -> predicted={predicted:8s} probs={np.round(probs, 3)}")


if __name__ == "__main__":
	main()

