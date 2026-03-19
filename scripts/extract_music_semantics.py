import argparse
import json
from pathlib import Path

import librosa
import numpy as np

try:
    import torch
    from transformers import AutoFeatureExtractor, AutoModel

    HAS_MERT = True
except Exception:
    torch = None
    AutoFeatureExtractor = None
    AutoModel = None
    HAS_MERT = False


def load_audio(file_path: Path, sample_rate: int = 24000) -> tuple[np.ndarray, int]:
    y, sr = librosa.load(file_path.as_posix(), sr=sample_rate, mono=True)
    if y.size == 0:
        raise ValueError("audio file is empty")
    return y, sr


def segment_audio(y: np.ndarray, sr: int, max_seconds: int) -> list[np.ndarray]:
    segment_length = max(sr * max_seconds, sr)
    if len(y) <= segment_length:
        return [y]
    return [y[offset : offset + segment_length] for offset in range(0, len(y), segment_length)]


def compute_low_level_hints(y: np.ndarray, sr: int) -> dict:
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
    tempo_value = float(np.atleast_1d(tempo)[0])
    rms = float(np.mean(librosa.feature.rms(y=y)[0]))
    centroid = float(np.mean(librosa.feature.spectral_centroid(y=y, sr=sr)[0]))
    chroma = librosa.feature.chroma_cqt(y=y, sr=sr) if y.size >= 4096 else librosa.feature.chroma_stft(y=y, sr=sr)
    chroma_mean = chroma.mean(axis=1)
    key_index = int(np.argmax(chroma_mean))
    mode = "minor" if chroma_mean[(key_index + 3) % 12] > chroma_mean[(key_index + 4) % 12] else "major"
    return {
        "tempo_bpm": round(tempo_value, 2),
        "energy_level": "high" if rms >= 0.08 else "medium" if rms >= 0.03 else "low",
        "spectral_brightness": round(centroid, 2),
        "key": ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"][key_index],
        "mode": mode,
    }


def build_candidate_labels(hints: dict) -> dict:
    moods = []
    genres = []
    tags = []

    tempo = hints["tempo_bpm"]
    energy = hints["energy_level"]
    bright = hints["spectral_brightness"]
    mode = hints["mode"]

    if energy == "high":
        moods.append({"label": "energetic", "score": 0.82})
        tags.append({"label": "driving", "score": 0.78})
    if energy == "low":
        moods.append({"label": "calm", "score": 0.76})
        tags.append({"label": "soft", "score": 0.7})
    if mode == "minor":
        moods.append({"label": "melancholic", "score": 0.74})
    else:
        moods.append({"label": "uplifting", "score": 0.68})
    if tempo >= 120 and bright >= 2200:
        genres.append({"label": "electronic", "score": 0.64})
        tags.append({"label": "bright", "score": 0.66})
    elif tempo >= 105:
        genres.append({"label": "pop", "score": 0.61})
    elif tempo < 90 and energy == "low":
        genres.append({"label": "ambient", "score": 0.58})
        tags.append({"label": "atmospheric", "score": 0.69})
    else:
        genres.append({"label": "indie", "score": 0.56})

    if bright < 1800:
        tags.append({"label": "warm", "score": 0.62})
    if tempo >= 128:
        tags.append({"label": "danceable", "score": 0.72})

    return {
        "mood": moods[:3],
        "genre": genres[:2],
        "tags": tags[:6],
    }


def extract_embedding(model_id: str, y: np.ndarray, sr: int, max_seconds: int, device: str) -> dict:
    if not HAS_MERT:
        return {
            "available": False,
            "model": model_id,
            "vector": [],
            "window_count": 0,
            "hidden_size": 0,
        }

    actual_device = device
    if actual_device == "auto":
        actual_device = "cuda" if torch.cuda.is_available() else "cpu"

    extractor = AutoFeatureExtractor.from_pretrained(model_id)
    model = AutoModel.from_pretrained(model_id)
    model.to(actual_device)
    model.eval()

    pooled_vectors = []
    for segment in segment_audio(y, sr, max_seconds):
        inputs = extractor(segment, sampling_rate=sr, return_tensors="pt")
        inputs = {key: value.to(actual_device) for key, value in inputs.items()}
        with torch.no_grad():
            outputs = model(**inputs)
        pooled = outputs.last_hidden_state.mean(dim=1).squeeze(0).detach().cpu().numpy()
        pooled_vectors.append(pooled)

    mean_vector = np.mean(np.stack(pooled_vectors), axis=0)
    return {
        "available": True,
        "model": model_id,
        "device": actual_device,
        "window_count": len(pooled_vectors),
        "hidden_size": int(mean_vector.shape[0]),
        "vector": [round(float(value), 6) for value in mean_vector.tolist()],
        "norm": round(float(np.linalg.norm(mean_vector)), 6),
    }


def analyze(file_path: Path, model_id: str, device: str, max_seconds: int) -> dict:
    y, sr = load_audio(file_path)
    hints = compute_low_level_hints(y, sr)
    embedding = extract_embedding(model_id, y, sr, max_seconds, device)
    labels = build_candidate_labels(hints)

    return {
        "analyzer": {
            "provider": "mert",
            "model": model_id,
            "mert_available": HAS_MERT,
            "labels_mode": "heuristic-bootstrap",
        },
        "file": {
            "file_name": file_path.name,
            "sample_rate": sr,
            "duration_seconds": round(float(librosa.get_duration(y=y, sr=sr)), 3),
        },
        "embedding": embedding,
        "audio_summary": hints,
        "mood": labels["mood"],
        "genre": labels["genre"],
        "tags": labels["tags"],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract MERT embeddings and bootstrap semantic labels.")
    parser.add_argument("file", type=Path, help="Path to input audio file")
    parser.add_argument("--model-id", default="m-a-p/MERT-v1-95M", help="Hugging Face model id")
    parser.add_argument("--device", default="auto", help="Inference device auto/cpu/cuda")
    parser.add_argument("--max-seconds", type=int, default=30, help="Window length for long tracks")
    args = parser.parse_args()

    result = analyze(args.file.resolve(), args.model_id, args.device, args.max_seconds)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
