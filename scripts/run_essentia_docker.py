import argparse
import os
import subprocess
import sys
from pathlib import Path


IMAGE_NAME = os.environ.get("YINYUE_ESSENTIA_IMAGE", "yinyue/essentia-runner:local")


def repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def relative_to_repo(path: Path) -> str:
    return path.resolve().relative_to(repo_root()).as_posix()


def image_exists() -> bool:
    result = subprocess.run(
        ["docker", "image", "inspect", IMAGE_NAME],
        capture_output=True,
        text=True,
    )
    return result.returncode == 0


def ensure_image() -> None:
    if image_exists():
        return

    dockerfile = repo_root() / "scripts" / "essentia.Dockerfile"
    result = subprocess.run(
        [
            "docker",
            "build",
            "-t",
            IMAGE_NAME,
            "-f",
            dockerfile.as_posix(),
            repo_root().as_posix(),
        ],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        details = (result.stderr or result.stdout or "").strip()
        raise RuntimeError(f"failed to build Essentia Docker image: {details}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Run Essentia-based analysis inside Docker.")
    parser.add_argument("file", type=Path, help="Path to audio file")
    parser.add_argument("--provider", default="essentia", help="Ignored, kept for compatibility")
    parser.add_argument("--stems-dir", type=Path, help="Optional directory of separated stems")
    args = parser.parse_args()

    target_file = args.file.resolve()
    if not target_file.exists():
        raise FileNotFoundError(f"audio file not found: {target_file}")

    ensure_image()

    mounted_root = repo_root().resolve()
    container_file = f"/work/{relative_to_repo(target_file)}"
    command = [
        "docker",
        "run",
        "--rm",
        "--network",
        "none",
        "-v",
        f"{mounted_root.as_posix()}:/work",
        IMAGE_NAME,
        container_file,
    ]

    if args.stems_dir:
        stems_dir = args.stems_dir.resolve()
        if not stems_dir.exists():
            raise FileNotFoundError(f"stems dir not found: {stems_dir}")
        command.extend(["--stems-dir", f"/work/{relative_to_repo(stems_dir)}"])

    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        stderr = result.stderr.strip()
        stdout = result.stdout.strip()
        raise RuntimeError(stderr or stdout or "essentia docker runner failed")

    sys.stdout.write(result.stdout)


if __name__ == "__main__":
    main()
