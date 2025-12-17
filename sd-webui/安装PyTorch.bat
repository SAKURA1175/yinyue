@echo off
REM ====================================================
REM PyTorch 单独安装脚本
REM 如果一键部署失败，用这个脚本单独安装 PyTorch
REM ====================================================

setlocal enabledelayedexpansion

cd /d %~dp0stable-diffusion-webui

echo.
echo ======================================
echo   PyTorch 安装脚本
echo ======================================
echo.

REM 激活虚拟环境
echo 激活虚拟环境...
call venv\Scripts\activate.bat

REM 尝试多个源安装 PyTorch
echo.
echo 尝试安装 PyTorch...
echo.

echo [1] 尝试从官方源下载...
python -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124 --no-cache-dir
if %errorlevel% equ 0 (
    echo ✓ 安装成功
    goto done
)

echo.
echo [2] 官方源失败，尝试国内镜像（阿里云）...
python -m pip install torch torchvision torchaudio -i https://mirrors.aliyun.com/pypi/simple/ --no-cache-dir
if %errorlevel% equ 0 (
    echo ✓ 安装成功
    goto done
)

echo.
echo [3] 继续尝试清华大学镜像...
python -m pip install torch torchvision torchaudio -i https://pypi.tsinghua.edu.cn/simple --no-cache-dir
if %errorlevel% equ 0 (
    echo ✓ 安装成功
    goto done
)

echo.
echo [4] 最后尝试豆瓣镜像...
python -m pip install torch torchvision torchaudio -i https://pypi.douban.com/simple --no-cache-dir
if %errorlevel% equ 0 (
    echo ✓ 安装成功
    goto done
)

:failed
echo.
echo ❌ 所有源都安装失败
echo.
echo 可能原因：
echo 1. 网络连接问题
echo 2. PyPI 镜像源故障
echo 3. 磁盘空间不足
echo.
echo 解决方案：
echo 1. 检查网络连接
echo 2. 稍后重试
echo 3. 清理临时文件并重新运行
echo 4. 尝试使用梯子加速下载
echo.
pause
exit /b 1

:done
echo.
echo ======================================
echo   ✓ PyTorch 安装完成
echo ======================================
echo.
echo 现在可以继续安装其他依赖
echo.
echo 运行以下命令继续：
echo   pip install -r requirements.txt
echo.
pause
