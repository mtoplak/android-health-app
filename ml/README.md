# TFLite Health Classifier (Option 2)

This folder contains scripts to train and export a real TensorFlow Lite model
for health classification from three inputs:

- heart rate (`srcniUtrip`)
- SpO2 (`spO2`)
- body temperature (`temperatura`)

## Files

- `train_health_classifier.py` - trains Keras model and exports `.tflite`
- `test_tflite_model.py` - runs quick inference sanity check
- `requirements.txt` - Python dependencies

## Output

The training script writes files into Android assets:

- `app/src/main/assets/health_classifier.tflite`
- `app/src/main/assets/health_classifier.meta.json`

Class index order is fixed to match the Android app:

- `0 = NORMAL`
- `1 = ELEVATED`
- `2 = CRITICAL`

## Run locally (Windows)

```powershell
Set-Location "D:\1\TZVA\android-health-app"
py -m pip install -r .\ml\requirements.txt
py .\ml\train_health_classifier.py
py .\ml\test_tflite_model.py
```

## If your machine has low disk space

Use Google Colab:

1. Upload `train_health_classifier.py`.
2. Run training and export.
3. Download `health_classifier.tflite`.
4. Copy it to `app/src/main/assets/health_classifier.tflite`.

