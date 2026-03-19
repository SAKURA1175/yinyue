@echo off
setlocal

echo [1/2] Installing frontend dependencies...
pushd frontend
call npm install
if errorlevel 1 goto :fail
popd

echo [2/2] Resolving backend dependencies...
pushd backend
call mvn -q -DskipTests dependency:go-offline
if errorlevel 1 goto :fail
popd

echo Setup complete.
exit /b 0

:fail
echo Setup failed.
exit /b 1
