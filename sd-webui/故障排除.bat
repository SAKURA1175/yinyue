@echo off
REM ====================================================
REM Stable Diffusion WebUI 故障排除脚本
REM ====================================================

setlocal enabledelayedexpansion

cd /d %~dp0stable-diffusion-webui

echo.
echo ======================================
echo   Stable Diffusion WebUI 故障诊断
echo ======================================
echo.

REM 检查虚拟环境
echo [1/4] 检查虚拟环境...
if not exist venv (
    echo ❌ 虚拟环境不存在
    echo 解决: 重新运行 "一键部署SD.bat"
    pause
    exit /b 1
)
echo ✓ 虚拟环境存在

REM 激活虚拟环境
call venv\Scripts\activate.bat

REM 检查 PyTorch
echo.
echo [2/4] 检查 PyTorch...
python -c "import torch; print(f'PyTorch {torch.__version__}'); print(f'CUDA 可用: {torch.cuda.is_available()}'); print(f'GPU: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else \"无\"}')" 2>nul
if errorlevel 1 (
    echo ❌ PyTorch 未正确安装
    echo 解决: 重新运行虚拟环境设置
    pause
    exit /b 1
)

REM 检查依赖
echo.
echo [3/4] 检查依赖...
python -c "import requests, PIL, numpy, safetensors, transformers" 2>nul
if errorlevel 1 (
    echo ❌ 缺少必要的依赖
    echo 正在安装...
    pip install requests pillow numpy safetensors transformers --quiet
)
echo ✓ 依赖完整

REM 检查模型
echo.
echo [4/4] 检查模型...
if exist models\Stable-diffusion (
    set COUNT=0
    for %%f in (models\Stable-diffusion\*) do set /a COUNT+=1
    if !COUNT! gtr 0 (
        echo ✓ 检测到 !COUNT! 个模型
    ) else (
        echo ⚠️ 模型目录为空，启动时会自动下载
    )
) else (
    echo ℹ️ 模型目录不存在，启动时会自动创建和下载
)

echo.
echo ======================================
echo   诊断完成
echo ======================================
echo.
echo 接下来:
echo 1. 运行 "快速启动SD.bat" 启动 WebUI
echo 2. 如果仍有问题，请检查网络连接和磁盘空间
echo.
pause
