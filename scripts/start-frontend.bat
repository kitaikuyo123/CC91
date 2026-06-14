@echo off
echo ========================================
echo   CC91 Frontend Startup
echo ========================================
echo.

cd /d %~dp0..\frontend

if not exist "node_modules" (
    echo [install] npm install ...
    call npm install
    echo.
)

echo [start] http://localhost:5173
echo   API proxy: http://localhost:9000 (Gateway)
echo.
call npm run dev
pause
