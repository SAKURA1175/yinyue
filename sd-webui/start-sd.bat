@echo off
REM ====================================================
REM Stable Diffusion WebUI 启动脚本 (RTX 4060 优化版)
REM ====================================================

setlocal enabledelayedexpansion

cd /d %~dp0stable-diffusion-webui

REM 检查 Python 环境
python --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ❌ 错误: 未检测到 Python
    echo 请先安装 Python 3.10+ 并加入 PATH
    echo.
    pause
    exit /b 1
)

echo.
echo ======================================
echo   Stable Diffusion WebUI 启动中...
echo ======================================
echo.
echo 配置:
echo   - 显卡: RTX 4060 (建议设置)
echo   - API 模式: 启用
echo   - 端口: 7860
echo.

REM 设置环境变量用于 GPU 优化
set CUDA_VISIBLE_DEVICES=0
REM set TORCH_CUDNN_BENCHMARK=1

REM 启动 WebUI（启用 API 模式）
REM 参数说明:
REM   --api: 启用 API 模式
REM   --listen: 监听所有网络接口
REM   --no-half: 禁用 half precision（某些 GPU 可能需要）
REM   --opt-sdp-attention: 启用优化的注意力机制

python launch.py --api --listen 0.0.0.0

pause
