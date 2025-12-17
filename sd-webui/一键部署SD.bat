@echo off
chcp 65001 >nul
echo ========================================================
echo       AI Music Project - Stable Diffusion Portable
echo ========================================================
echo.

set "dp0=%~dp0"
set "REPO_DIR=%dp0%stable-diffusion-webui"
set "PYTHON=%REPO_DIR%\python\python.exe"
set "GIT=git"
set "VENV_DIR=-"
set "COMMANDLINE_ARGS=--api --listen --xformers --no-half-vae"

:: 1. Check Git
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Git is not installed.
    echo Please install Git from: https://git-scm.com/download/win
    pause
    exit /b
)
echo [OK] Git found.

:: 2. Clone Repository (if needed)
if not exist "%REPO_DIR%\launch.py" (
    echo [INFO] Cloning Stable Diffusion WebUI...
    git clone https://github.com/AUTOMATIC1111/stable-diffusion-webui.git
) else (
    echo [OK] Repository already exists.
)

:: 3. Check Portable Python
if not exist "%PYTHON%" (
    echo [ERROR] Portable Python not found at %PYTHON%
    echo Please ensure you ran the setup correctly.
    pause
    exit /b
)
echo [OK] Using Portable Python: %PYTHON%

:: 4. Launch
echo [INFO] Starting Stable Diffusion WebUI...
echo [TIP] First run will download models (can take time).
echo.

cd /d "%REPO_DIR%"
"%PYTHON%" launch.py %COMMANDLINE_ARGS%

pause
