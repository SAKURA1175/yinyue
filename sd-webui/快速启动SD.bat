@echo off
set "dp0=%~dp0"
set "REPO_DIR=%dp0%stable-diffusion-webui"
set "PYTHON=%REPO_DIR%\python\python.exe"
set "VENV_DIR=-"
set "HF_ENDPOINT=https://hf-mirror.com"
set "COMMANDLINE_ARGS=--api --listen --xformers --no-half-vae --skip-install --no-download-sd-model"

if not exist "%PYTHON%" (
    echo [ERROR] Portable Python not found at %PYTHON%
    pause
    exit /b
)

if not exist "%REPO_DIR%\launch.py" (
    echo [ERROR] stable-diffusion-webui directory not found.
    pause
    exit /b
)

echo [INFO] Using Portable Python: %PYTHON%
echo [INFO] Starting WebUI...

cd /d "%REPO_DIR%"
"%PYTHON%" launch.py %COMMANDLINE_ARGS%

pause
