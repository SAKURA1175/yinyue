import argparse
import json
import subprocess
import sys
import time
import uuid
from pathlib import Path


def build_command(audio_file: Path, output_root: Path, model: str, device: str, two_stems: str | None) -> list[str]:
    command = [sys.executable, "-m", "demucs.separate", "--out", output_root.as_posix(), "-n", model]
    if device and device != "auto":
        command.extend(["-d", device])
    if two_stems:
        command.extend(["--two-stems", two_stems])
    command.append(audio_file.as_posix())
    return command


def locate_track_dir(run_root: Path, model: str, audio_file: Path) -> Path:
    model_dir = run_root / model
    exact = model_dir / audio_file.stem
    if exact.exists():
        return exact

    candidates = [item for item in model_dir.iterdir() if item.is_dir()] if model_dir.exists() else []
    if not candidates:
        raise FileNotFoundError("demucs output directory not found")
    return max(candidates, key=lambda item: item.stat().st_mtime)


def separate(audio_file: Path, output_root: Path, model: str, device: str, two_stems: str | None) -> dict:
    run_id = uuid.uuid4().hex[:12]
    run_root = output_root / run_id
    run_root.mkdir(parents=True, exist_ok=True)

    command = build_command(audio_file, run_root, model, device, two_stems)
    started_at = time.time()
    process = subprocess.run(command, capture_output=True, text=True)
    duration_ms = int((time.time() - started_at) * 1000)

    if process.returncode != 0:
        raise RuntimeError((process.stderr or process.stdout or "demucs failed").strip())

    track_dir = locate_track_dir(run_root, model, audio_file)
    stems = {}
    for stem_file in sorted(track_dir.glob("*.wav")):
        stems[stem_file.stem] = stem_file.resolve().as_posix()

    return {
        "status": "ok",
        "model": model,
        "device": device,
        "run_id": run_id,
        "output_dir": track_dir.resolve().as_posix(),
        "stems": stems,
        "duration_ms": duration_ms,
        "stdout": (process.stdout or "").strip(),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Run Demucs source separation and emit JSON.")
    parser.add_argument("file", type=Path, help="Path to input audio file")
    parser.add_argument("--output-root", type=Path, required=True, help="Root directory for separated outputs")
    parser.add_argument("--model", default="htdemucs", help="Demucs model name")
    parser.add_argument("--device", default="auto", help="Demucs device, e.g. auto/cpu/cuda")
    parser.add_argument("--two-stems", help="Optional two-stem target, e.g. vocals")
    args = parser.parse_args()

    result = separate(args.file.resolve(), args.output_root.resolve(), args.model, args.device, args.two_stems)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
