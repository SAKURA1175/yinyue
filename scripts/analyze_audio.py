import argparse
import importlib
import json
import warnings
from pathlib import Path

import librosa  # type: ignore
import numpy as np

try:
    importlib.import_module("essentia.standard")
    HAS_ESSENTIA = True
except Exception:
    HAS_ESSENTIA = False


PITCH_CLASSES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
MAJOR_PROFILE = np.array([6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88])
MINOR_PROFILE = np.array([6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17])


def classify_energy(rms_mean: float) -> str:
    if rms_mean < 0.03:
        return "low"
    if rms_mean < 0.08:
        return "medium"
    return "high"


def estimate_key(chroma_mean: np.ndarray) -> dict:
    chroma_norm = chroma_mean / (np.linalg.norm(chroma_mean) + 1e-8)
    best_label = "Unknown"
    best_mode = "unknown"
    best_score = -1.0

    for idx, pitch in enumerate(PITCH_CLASSES):
        major_score = float(np.dot(chroma_norm, np.roll(MAJOR_PROFILE, idx) / np.linalg.norm(MAJOR_PROFILE)))
        minor_score = float(np.dot(chroma_norm, np.roll(MINOR_PROFILE, idx) / np.linalg.norm(MINOR_PROFILE)))

        if major_score > best_score:
            best_score = major_score
            best_label = pitch
            best_mode = "major"

        if minor_score > best_score:
            best_score = minor_score
            best_label = pitch
            best_mode = "minor"

    return {
        "key": best_label,
        "scale": best_mode,
        "mode": best_mode,
        "confidence": round(best_score, 4),
    }


def load_audio(file_path: Path, sample_rate: int = 22050) -> tuple[np.ndarray, int]:
    y, sr = librosa.load(file_path.as_posix(), sr=sample_rate, mono=True)
    if y.size == 0:
        raise ValueError("audio file is empty")
    return y, sr


def build_low_level_profile(file_path: Path) -> dict:
    y, sr = load_audio(file_path)
    duration = float(librosa.get_duration(y=y, sr=sr))
    tempo, beat_frames = librosa.beat.beat_track(y=y, sr=sr)
    tempo_value = float(np.atleast_1d(tempo)[0])
    beat_times = librosa.frames_to_time(beat_frames, sr=sr)
    rms = librosa.feature.rms(y=y)[0]
    zcr = librosa.feature.zero_crossing_rate(y)[0]
    spectral_centroid = librosa.feature.spectral_centroid(y=y, sr=sr)[0]
    spectral_bandwidth = librosa.feature.spectral_bandwidth(y=y, sr=sr)[0]
    rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)[0]

    with warnings.catch_warnings():
        warnings.filterwarnings("ignore", message="n_fft=.*too large for input signal")
        if y.size >= 4096:
            chroma = librosa.feature.chroma_cqt(y=y, sr=sr)
        else:
            n_fft = max(512, 2 ** int(np.floor(np.log2(max(y.size, 512)))))
            chroma = librosa.feature.chroma_stft(y=y, sr=sr, n_fft=n_fft)

    chroma_mean = chroma.mean(axis=1)
    key_info = estimate_key(chroma_mean)
    top_pitch_indices = np.argsort(chroma_mean)[::-1][:3]
    dominant_pitches = [PITCH_CLASSES[int(index)] for index in top_pitch_indices]

    return {
        "sample_rate": sr,
        "duration_seconds": round(duration, 3),
        "rhythm": {
            "bpm": round(tempo_value, 2),
            "confidence": None,
            "beat_count": int(len(beat_frames)),
            "beat_positions_seconds": [round(float(value), 3) for value in beat_times[:64]],
            "tempo_stable": len(beat_frames) > 0,
        },
        "energy": {
            "rms_mean": round(float(np.mean(rms)), 5),
            "rms_std": round(float(np.std(rms)), 5),
            "level": classify_energy(float(np.mean(rms))),
        },
        "spectral": {
            "centroid_mean": round(float(np.mean(spectral_centroid)), 2),
            "bandwidth_mean": round(float(np.mean(spectral_bandwidth)), 2),
            "rolloff_mean": round(float(np.mean(rolloff)), 2),
            "zero_crossing_rate_mean": round(float(np.mean(zcr)), 5),
        },
        "tonal": {
            **key_info,
            "strength": key_info["confidence"],
            "dominant_pitches": dominant_pitches,
            "pitch_class_profile": [round(float(value), 5) for value in chroma_mean.tolist()],
        },
        "danceability": {
            "score": heuristic_danceability(tempo_value, float(np.mean(rms)), float(np.mean(spectral_centroid))),
            "confidence": 0.35,
        },
        "chords": {
            "sequence": [],
            "histogram": [],
            "changes_per_minute": 0.0,
        },
    }


def heuristic_danceability(tempo: float, rms_mean: float, centroid_mean: float) -> float:
    tempo_score = 1.0 - min(abs(tempo - 122.0) / 122.0, 1.0)
    energy_score = min(max(rms_mean / 0.12, 0.0), 1.0)
    brightness_score = min(max(centroid_mean / 3500.0, 0.0), 1.0)
    return round(float((tempo_score * 0.45) + (energy_score * 0.4) + (brightness_score * 0.15)), 4)


def extract_chords_with_essentia(audio: np.ndarray, sample_rate: int) -> dict:
    if not HAS_ESSENTIA:
        return {
            "sequence": [],
            "histogram": [],
            "changes_per_minute": 0.0,
        }

    import essentia.standard as es_runtime  # type: ignore

    frame_size = 4096
    hop_size = 2048
    window = es_runtime.Windowing(type="hann")
    spectrum = es_runtime.Spectrum()
    spectral_peaks = es_runtime.SpectralPeaks()
    hpcp = es_runtime.HPCP()
    hpcp_frames = []

    for frame in es_runtime.FrameGenerator(audio, frameSize=frame_size, hopSize=hop_size, startFromZero=True):
        spec = spectrum(window(frame))
        freqs, mags = spectral_peaks(spec)
        if len(freqs) == 0:
            continue
        hpcp_frames.append(hpcp(freqs, mags))

    if not hpcp_frames:
        return {
            "sequence": [],
            "histogram": [],
            "changes_per_minute": 0.0,
        }

    chords_detection = es_runtime.ChordsDetection()
    chord_labels, strengths = chords_detection(np.array(hpcp_frames))
    seconds_per_frame = hop_size / float(sample_rate)
    sequence = []
    histogram = {}
    previous_label = None
    changes = 0

    for index, label in enumerate(chord_labels):
        label_value = str(label)
        histogram[label_value] = histogram.get(label_value, 0) + 1
        if label_value != previous_label:
            if previous_label is not None:
                changes += 1
            sequence.append({
                "start": round(index * seconds_per_frame, 3),
                "end": round((index + 1) * seconds_per_frame, 3),
                "label": label_value,
                "confidence": round(float(strengths[index]), 4) if len(strengths) > index else None,
            })
            previous_label = label_value
        else:
            sequence[-1]["end"] = round((index + 1) * seconds_per_frame, 3)

    total_frames = max(len(chord_labels), 1)
    histogram_items = [
        {"label": label, "ratio": round(count / total_frames, 4)}
        for label, count in sorted(histogram.items(), key=lambda item: item[1], reverse=True)[:12]
    ]
    duration_minutes = max((len(chord_labels) * seconds_per_frame) / 60.0, 1e-6)

    return {
        "sequence": sequence[:64],
        "histogram": histogram_items,
        "changes_per_minute": round(changes / duration_minutes, 3),
    }


def build_essentia_profile(file_path: Path) -> dict:
    if not HAS_ESSENTIA:
        raise RuntimeError("essentia is not installed")

    import essentia.standard as es_runtime  # type: ignore

    low_level = build_low_level_profile(file_path)
    audio = es_runtime.MonoLoader(filename=file_path.as_posix(), sampleRate=44100)()
    rhythm = es_runtime.RhythmExtractor2013(method="multifeature")
    bpm, beats, beats_confidence, _, _ = rhythm(audio)
    key, scale, key_strength = es_runtime.KeyExtractor()(audio)
    danceability, _ = es_runtime.Danceability()(audio)

    low_level["sample_rate"] = 44100
    low_level["rhythm"]["bpm"] = round(float(bpm), 2)
    low_level["rhythm"]["confidence"] = round(float(beats_confidence), 4)
    low_level["rhythm"]["beat_count"] = len(beats)
    low_level["rhythm"]["beat_positions_seconds"] = [round(float(value), 3) for value in beats[:64]]
    low_level["tonal"]["key"] = str(key)
    low_level["tonal"]["scale"] = str(scale)
    low_level["tonal"]["mode"] = str(scale)
    low_level["tonal"]["confidence"] = round(float(key_strength), 4)
    low_level["tonal"]["strength"] = round(float(key_strength), 4)
    low_level["danceability"]["score"] = round(float(danceability), 4)
    low_level["danceability"]["confidence"] = 0.8
    low_level["chords"] = extract_chords_with_essentia(audio, 44100)
    return low_level


def analyze_stems(stems_dir: Path) -> dict:
    if not stems_dir.exists() or not stems_dir.is_dir():
        return {}

    result = {}
    for stem_file in sorted(stems_dir.glob("*.wav")):
        profile = build_low_level_profile(stem_file)
        result[stem_file.stem] = {
            "rhythm": profile["rhythm"],
            "energy": profile["energy"],
            "spectral": profile["spectral"],
            "tonal": {
                "key": profile["tonal"]["key"],
                "scale": profile["tonal"]["scale"],
                "confidence": profile["tonal"]["confidence"],
            },
        }
    return result


def analyze_audio(file_path: Path, provider: str, stems_dir: Path | None) -> dict:
    requested_provider = provider.lower()
    use_essentia = requested_provider == "essentia" or (requested_provider == "auto" and HAS_ESSENTIA)

    if use_essentia:
        profile = build_essentia_profile(file_path)
        provider_name = "essentia"
    else:
        profile = build_low_level_profile(file_path)
        provider_name = "librosa"

    return {
        "analyzer": {
            "provider": provider_name,
            "requested_provider": requested_provider,
            "essentia_available": HAS_ESSENTIA,
            "features_version": "v2",
        },
        "file": {
            "file_name": file_path.name,
            "duration_seconds": profile["duration_seconds"],
            "sample_rate": profile["sample_rate"],
        },
        "rhythm": profile["rhythm"],
        "tonal": profile["tonal"],
        "chords": profile["chords"],
        "danceability": profile["danceability"],
        "energy": profile["energy"],
        "spectral": profile["spectral"],
        "stems": analyze_stems(stems_dir) if stems_dir else {},
        "aliases": {
            "bpm": profile["rhythm"]["bpm"],
            "key": f"{profile['tonal']['key']} {profile['tonal']['scale']}".strip(),
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract music features from an audio file.")
    parser.add_argument("file", type=Path, help="Path to the audio file")
    parser.add_argument("--provider", default="auto", choices=["auto", "essentia", "librosa"], help="Feature provider")
    parser.add_argument("--stems-dir", type=Path, help="Optional directory containing separated stems")
    args = parser.parse_args()

    result = analyze_audio(args.file.resolve(), args.provider, args.stems_dir.resolve() if args.stems_dir else None)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
