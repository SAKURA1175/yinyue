@echo off
REM ====================================================
REM React + Vite 项目初始化脚本
REM ====================================================

echo.
echo ======================================
echo   初始化 React + Vite 项目...
echo ======================================
echo.

REM 检查 Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到 Node.js
    echo 请先安装 Node.js 18+ 并加入 PATH
    echo 下载地址: https://nodejs.org/
    pause
    exit /b 1
)

REM 创建 React 项目
echo 正在创建 React + Vite 项目...
call npm create vite@latest . -- --template react
if errorlevel 1 (
    echo ❌ 创建项目失败
    pause
    exit /b 1
)

echo.
echo 正在安装依赖...
call npm install

REM 安装额外的依赖
echo.
echo 正在安装额外库...
call npm install axios zustand react-router-dom @tailwindcss/typography tailwindcss postcss autoprefixer

REM 初始化 Tailwind CSS
echo.
echo 正在配置 Tailwind CSS...
call npx tailwindcss init -p

echo.
echo ======================================
echo   ✓ 项目初始化完成！
echo ======================================
echo.
echo 启动开发服务器:
echo   npm run dev
echo.
pause
